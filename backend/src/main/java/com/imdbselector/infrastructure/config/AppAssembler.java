package com.imdbselector.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.imdbselector.application.usecase.MediaSelectionService;
import com.imdbselector.application.usecase.UserService;
import com.imdbselector.application.usecase.WatchlistService;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.InvalidCsvException;
import com.imdbselector.domain.exception.NoMatchingMediaException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.service.RandomMediaSelector;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.infrastructure.adapter.in.http.ApiDtos;
import com.imdbselector.infrastructure.adapter.in.http.SelectionController;
import com.imdbselector.infrastructure.adapter.in.http.UserController;
import com.imdbselector.infrastructure.adapter.in.http.WatchlistController;
import com.imdbselector.infrastructure.adapter.out.csv.OpenCsvWatchlistParser;
import com.imdbselector.infrastructure.adapter.out.persistence.DatabaseConfig;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqMediaItemRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqUserRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistRepository;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.time.Clock;
import java.util.Map;

/**
 * Manual dependency injection root wiring Domain, Application, and Infrastructure adapters.
 */
public final class AppAssembler implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final Javalin javalin;

    public AppAssembler(String jdbcUrl, String dbUser, String dbPassword, String allowedOrigin) {
        this.dataSource = DatabaseConfig.createDataSource(jdbcUrl, dbUser, dbPassword);
        DatabaseConfig.migrate(this.dataSource);
        var dsl = DatabaseConfig.createDsl(this.dataSource);

        var mediaRepo = new JooqMediaItemRepository(dsl);
        var userRepo = new JooqUserRepository(dsl);
        var watchlistRepo = new JooqWatchlistRepository(dsl);
        var csvParser = new OpenCsvWatchlistParser();

        var filterService = new WatchlistFilterService();
        var selector = new RandomMediaSelector();
        var clock = Clock.systemUTC();

        var userService = new UserService(userRepo, mediaRepo);
        var watchlistService = new WatchlistService(watchlistRepo, mediaRepo, userRepo, csvParser, filterService, clock);
        var selectionService = new MediaSelectionService(watchlistRepo, mediaRepo, userRepo, filterService, selector, clock);

        this.javalin = createJavalin(allowedOrigin, userService, watchlistService, selectionService);
    }

    private Javalin createJavalin(
            String allowedOrigin,
            UserService userService,
            WatchlistService watchlistService,
            MediaSelectionService selectionService
    ) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.jsonMapper(new JavalinJackson(mapper, false));
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                if ("*".equals(allowedOrigin)) {
                    rule.anyHost();
                } else {
                    rule.allowHost(allowedOrigin);
                }
            }));
        });

        app.get("/api/health", ctx -> ctx.json(Map.of("status", "UP")));

        new UserController(userService).registerRoutes(app);
        new WatchlistController(watchlistService).registerRoutes(app);
        new SelectionController(selectionService).registerRoutes(app);

        app.exception(ResourceNotFoundException.class, (e, ctx) ->
                ctx.status(404).json(new ApiDtos.ErrorResponse("NOT_FOUND", e.getMessage())));
        app.exception(NoMatchingMediaException.class, (e, ctx) ->
                ctx.status(404).json(new ApiDtos.ErrorResponse("NO_MATCHING_MEDIA", e.getMessage())));
        app.exception(DuplicateResourceException.class, (e, ctx) ->
                ctx.status(409).json(new ApiDtos.ErrorResponse("CONFLICT", e.getMessage())));
        app.exception(InvalidCsvException.class, (e, ctx) ->
                ctx.status(400).json(new ApiDtos.ErrorResponse("INVALID_CSV", e.getMessage())));
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400).json(new ApiDtos.ErrorResponse("BAD_REQUEST", e.getMessage())));

        return app;
    }

    public Javalin javalin() {
        return javalin;
    }

    @Override
    public void close() {
        javalin.stop();
        dataSource.close();
    }
}
