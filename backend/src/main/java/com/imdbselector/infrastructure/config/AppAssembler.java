package com.imdbselector.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.imdbselector.application.usecase.AuthService;
import com.imdbselector.application.usecase.MediaSelectionService;
import com.imdbselector.application.usecase.UserService;
import com.imdbselector.application.usecase.WatchlistService;
import com.imdbselector.domain.service.RandomMediaSelector;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.infrastructure.adapter.in.http.AuthController;
import com.imdbselector.infrastructure.adapter.in.http.HttpObservability;
import com.imdbselector.infrastructure.adapter.in.http.SelectionController;
import com.imdbselector.infrastructure.adapter.in.http.UserController;
import com.imdbselector.infrastructure.adapter.in.http.WatchlistController;
import com.imdbselector.infrastructure.adapter.out.csv.OpenCsvWatchlistParser;
import com.imdbselector.infrastructure.adapter.out.persistence.DatabaseConfig;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqMediaItemRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqUserRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistSubscriptionRepository;
import com.imdbselector.infrastructure.adapter.out.security.BcryptPasswordHasher;
import com.imdbselector.infrastructure.adapter.out.security.JwtTokenAdapter;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Manual dependency injection root wiring Domain, Application, and Infrastructure adapters.
 */
public final class AppAssembler implements AutoCloseable {
    private static final String DEV_FALLBACK_SECRET = "dev-only-change-me-in-prod-cinema-roulette-secret";
    private final HikariDataSource dataSource;
    private final Javalin javalin;

    public AppAssembler(String jdbcUrl, String dbUser, String dbPassword, String allowedOrigin) {
        this.dataSource = DatabaseConfig.createDataSource(jdbcUrl, dbUser, dbPassword);
        DatabaseConfig.migrate(this.dataSource);
        var dsl = DatabaseConfig.createDsl(this.dataSource);

        var mediaRepo = new JooqMediaItemRepository(dsl);
        var userRepo = new JooqUserRepository(dsl);
        var watchlistRepo = new JooqWatchlistRepository(dsl);
        var subscriptionRepo = new JooqWatchlistSubscriptionRepository(dsl);
        var csvParser = new OpenCsvWatchlistParser();

        var clock = Clock.systemUTC();
        String jwtSecret = System.getenv().getOrDefault("JWT_SECRET", DEV_FALLBACK_SECRET);
        var passwordHasher = new BcryptPasswordHasher();
        var tokenPort = new JwtTokenAdapter(jwtSecret, clock, Duration.ofDays(14));

        var filterService = new WatchlistFilterService();
        var selector = new RandomMediaSelector();

        var authService = new AuthService(userRepo, passwordHasher, tokenPort);
        var userService = new UserService(userRepo, mediaRepo);
        var watchlistService = new WatchlistService(
                watchlistRepo, subscriptionRepo, mediaRepo, userRepo, csvParser, filterService, clock);
        var selectionService = new MediaSelectionService(
                watchlistRepo, subscriptionRepo, mediaRepo, userRepo, filterService, selector, clock);

        this.javalin = createJavalin(allowedOrigin, authService, userService, watchlistService, selectionService);
    }

    private Javalin createJavalin(
            String allowedOrigin,
            AuthService authService,
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
                rule.allowCredentials = true;
                if ("*".equals(allowedOrigin)) {
                    rule.reflectClientOrigin = true;
                } else {
                    rule.allowHost(allowedOrigin);
                }
            }));
        });

        HttpObservability.register(app);

        app.get("/api/health", ctx -> ctx.json(Map.of("status", "UP")));

        var authController = new AuthController(authService, userService);
        authController.registerRoutes(app);
        new UserController(userService, authController).registerRoutes(app);
        new WatchlistController(watchlistService, authController).registerRoutes(app);
        new SelectionController(selectionService, authController).registerRoutes(app);

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
