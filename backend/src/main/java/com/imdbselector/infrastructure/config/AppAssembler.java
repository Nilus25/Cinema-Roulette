package com.imdbselector.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.imdbselector.application.usecase.auth.AuthenticateTokenUseCase;
import com.imdbselector.application.usecase.auth.ChangePasswordUseCase;
import com.imdbselector.application.usecase.auth.LoginUserUseCase;
import com.imdbselector.application.usecase.auth.RegisterUserUseCase;
import com.imdbselector.application.usecase.selection.SelectRandomMediaUseCase;
import com.imdbselector.application.usecase.user.DeleteUserUseCase;
import com.imdbselector.application.usecase.user.ExportUserDataUseCase;
import com.imdbselector.application.usecase.user.GetUserDetailsUseCase;
import com.imdbselector.application.usecase.user.MarkMediaSeenUseCase;
import com.imdbselector.application.usecase.user.UnmarkMediaSeenUseCase;
import com.imdbselector.application.usecase.watchlist.ChangeWatchlistVisibilityUseCase;
import com.imdbselector.application.usecase.watchlist.DeleteWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.GetWatchlistItemsUseCase;
import com.imdbselector.application.usecase.watchlist.ImportWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.ListAccessibleWatchlistsUseCase;
import com.imdbselector.application.usecase.watchlist.ListPublicWatchlistsUseCase;
import com.imdbselector.application.usecase.watchlist.PreviewWatchlistUpdateUseCase;
import com.imdbselector.application.usecase.watchlist.SubscribeWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.UnsubscribeWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.UpdateWatchlistUseCase;
import com.imdbselector.domain.model.CinemaRoulette;
import com.imdbselector.infrastructure.adapter.in.http.AuthController;
import com.imdbselector.infrastructure.adapter.in.http.HttpObservability;
import com.imdbselector.infrastructure.adapter.in.http.RateLimitFilter;
import com.imdbselector.infrastructure.adapter.in.http.SecurityHeaders;
import com.imdbselector.infrastructure.adapter.in.http.SelectionController;
import com.imdbselector.infrastructure.adapter.in.http.UserController;
import com.imdbselector.infrastructure.adapter.in.http.WatchlistController;
import com.imdbselector.infrastructure.adapter.out.csv.OpenCsvWatchlistParser;
import com.imdbselector.infrastructure.adapter.out.persistence.DatabaseConfig;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqMediaItemRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqTransactionManager;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqUserRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistSubscriptionRepository;
import com.imdbselector.infrastructure.adapter.out.security.BcryptPasswordHasher;
import com.imdbselector.infrastructure.adapter.out.security.JwtTokenAdapter;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.jooq.DSLContext;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Manual dependency injection root wiring Domain, Application UseCases, and Infrastructure adapters.
 */
public final class AppAssembler implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final Javalin javalin;

    public AppAssembler(String jdbcUrl, String dbUser, String dbPassword, String allowedOrigin) {
        SecuritySettings settings = SecuritySettings.fromEnv(System.getenv(), allowedOrigin);
        this.dataSource = DatabaseConfig.createDataSource(jdbcUrl, dbUser, dbPassword);
        DatabaseConfig.migrate(this.dataSource);
        JooqTransactionManager txManager = new JooqTransactionManager(this.dataSource);
        DSLContext dsl = DatabaseConfig.createDsl(txManager);

        JooqMediaItemRepository mediaRepo = new JooqMediaItemRepository(dsl);
        JooqUserRepository userRepo = new JooqUserRepository(dsl);
        JooqWatchlistRepository watchlistRepo = new JooqWatchlistRepository(dsl);
        JooqWatchlistSubscriptionRepository subscriptionRepo = new JooqWatchlistSubscriptionRepository(dsl);
        OpenCsvWatchlistParser csvParser = new OpenCsvWatchlistParser();

        Clock clock = Clock.systemUTC();
        BcryptPasswordHasher passwordHasher = new BcryptPasswordHasher();
        JwtTokenAdapter tokenPort = new JwtTokenAdapter(settings.jwtSecret(), clock, Duration.ofDays(14));
        CinemaRoulette roulette = new CinemaRoulette();

        RegisterUserUseCase registerUser = new RegisterUserUseCase(userRepo, passwordHasher, tokenPort, txManager);
        LoginUserUseCase loginUser = new LoginUserUseCase(userRepo, passwordHasher, tokenPort);
        AuthenticateTokenUseCase authenticateToken = new AuthenticateTokenUseCase(tokenPort);
        ChangePasswordUseCase changePassword = new ChangePasswordUseCase(userRepo, passwordHasher, tokenPort);
        GetUserDetailsUseCase getUserDetails = new GetUserDetailsUseCase(userRepo, mediaRepo);
        ExportUserDataUseCase exportUserData = new ExportUserDataUseCase(
                userRepo, watchlistRepo, subscriptionRepo, mediaRepo, clock);
        DeleteUserUseCase deleteUser = new DeleteUserUseCase(userRepo, watchlistRepo, subscriptionRepo, txManager);
        MarkMediaSeenUseCase markMediaSeen = new MarkMediaSeenUseCase(userRepo);
        UnmarkMediaSeenUseCase unmarkMediaSeen = new UnmarkMediaSeenUseCase(userRepo);

        ImportWatchlistUseCase importWatchlist = new ImportWatchlistUseCase(watchlistRepo, mediaRepo, csvParser, txManager);
        PreviewWatchlistUpdateUseCase previewWatchlistUpdate = new PreviewWatchlistUpdateUseCase(watchlistRepo, mediaRepo, csvParser);
        UpdateWatchlistUseCase updateWatchlist = new UpdateWatchlistUseCase(watchlistRepo, mediaRepo, userRepo, csvParser, txManager, clock);
        DeleteWatchlistUseCase deleteWatchlist = new DeleteWatchlistUseCase(watchlistRepo, subscriptionRepo, txManager);
        ChangeWatchlistVisibilityUseCase changeVisibility = new ChangeWatchlistVisibilityUseCase(watchlistRepo, clock);
        ListAccessibleWatchlistsUseCase listAccessible = new ListAccessibleWatchlistsUseCase(watchlistRepo, subscriptionRepo, userRepo);
        ListPublicWatchlistsUseCase listPublic = new ListPublicWatchlistsUseCase(watchlistRepo, userRepo);
        GetWatchlistItemsUseCase getWatchlistItems = new GetWatchlistItemsUseCase(watchlistRepo, subscriptionRepo, mediaRepo, userRepo);
        SubscribeWatchlistUseCase subscribeWatchlist = new SubscribeWatchlistUseCase(watchlistRepo, subscriptionRepo);
        UnsubscribeWatchlistUseCase unsubscribeWatchlist = new UnsubscribeWatchlistUseCase(subscriptionRepo);
        SelectRandomMediaUseCase selectRandomMedia = new SelectRandomMediaUseCase(
                watchlistRepo, subscriptionRepo, mediaRepo, userRepo, txManager, roulette, clock);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.http.maxRequestSize = settings.maxUploadBytes();
            config.jsonMapper(new JavalinJackson(mapper, false));
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.allowCredentials = true;
                if ("*".equals(settings.corsOrigin())) {
                    rule.reflectClientOrigin = true;
                } else {
                    rule.allowHost(settings.corsOrigin());
                }
            }));
        });

        HttpObservability.register(app);
        SecurityHeaders.register(app);
        RateLimitFilter authRateLimiter = new RateLimitFilter(10, Duration.ofMinutes(1), clock);
        authRateLimiter.register(app, "/api/auth/login", "/api/auth/register", "/api/auth/password");
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "UP")));

        AuthController authController = new AuthController(
                registerUser, loginUser, authenticateToken, changePassword, getUserDetails, settings.cookieSecure());
        authController.registerRoutes(app);
        new UserController(getUserDetails, exportUserData, deleteUser, markMediaSeen, unmarkMediaSeen, authController).registerRoutes(app);
        new WatchlistController(
                importWatchlist,
                previewWatchlistUpdate,
                updateWatchlist,
                deleteWatchlist,
                changeVisibility,
                listAccessible,
                listPublic,
                getWatchlistItems,
                subscribeWatchlist,
                unsubscribeWatchlist,
                authController
        ).registerRoutes(app);
        new SelectionController(selectRandomMedia, authController).registerRoutes(app);

        this.javalin = app;
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
