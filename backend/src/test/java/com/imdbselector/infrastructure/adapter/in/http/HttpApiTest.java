package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.application.usecase.auth.AuthenticateTokenUseCase;
import com.imdbselector.application.usecase.auth.ChangePasswordUseCase;
import com.imdbselector.application.usecase.auth.LoginUserUseCase;
import com.imdbselector.application.usecase.auth.RegisterUserUseCase;
import com.imdbselector.application.usecase.user.DeleteUserUseCase;
import com.imdbselector.application.usecase.user.ExportUserDataUseCase;
import com.imdbselector.application.usecase.user.GetUserDetailsUseCase;
import com.imdbselector.application.usecase.user.MarkMediaSeenUseCase;
import com.imdbselector.application.usecase.user.UnmarkMediaSeenUseCase;
import com.imdbselector.application.usecase.user.UpdateQueryFlavorUseCase;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.infrastructure.adapter.out.security.BcryptPasswordHasher;
import com.imdbselector.infrastructure.adapter.out.security.JwtTokenAdapter;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HttpApiTest {

    @Test
    void registersLogsInAndProtectsEndpointsWith401AndSanitizes500() throws Exception {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        EmptyMediaRepo mediaRepo = new EmptyMediaRepo();
        BcryptPasswordHasher hasher = new BcryptPasswordHasher();
        JwtTokenAdapter tokenPort = new JwtTokenAdapter("test-secret-at-least-32-chars-long!", Clock.systemUTC(), Duration.ofHours(1));
        GetUserDetailsUseCase getDetails = new GetUserDetailsUseCase(userRepo, mediaRepo);

        Javalin app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json");
        HttpObservability.register(app);
        SecurityHeaders.register(app);
        new RateLimitFilter(5, Duration.ofMinutes(1), Clock.systemUTC()).register(app, "/api/auth/login");
        TransactionPort noopTx = new TransactionPort() {
            @Override public <T> T inTransaction(java.util.function.Supplier<T> work) { return work.get(); }
        };
        AuthController authController = new AuthController(
                new RegisterUserUseCase(userRepo, hasher, tokenPort, noopTx),
                new LoginUserUseCase(userRepo, hasher, tokenPort),
                new AuthenticateTokenUseCase(tokenPort),
                new ChangePasswordUseCase(userRepo, hasher, tokenPort),
                getDetails
        );
        authController.registerRoutes(app);
        WatchlistRepository emptyWlRepo = new WatchlistRepository() {
            @Override public List<Watchlist> findByOwner(UserId ownerId) { return List.of(); }
            @Override public List<Watchlist> findByIds(Collection<WatchlistId> ids) { return List.of(); }
            @Override public List<Watchlist> findPublicWatchlists() { return List.of(); }
            @Override public Optional<Watchlist> findById(WatchlistId id) { return Optional.empty(); }
            @Override public Optional<Watchlist> findByOwnerAndName(UserId ownerId, String name) { return Optional.empty(); }
            @Override public Optional<Watchlist> findByShareCode(String shareCode) { return Optional.empty(); }
            @Override public Watchlist save(Watchlist watchlist) { return watchlist; }
            @Override public void deleteById(WatchlistId id) {}
        };
        WatchlistSubscriptionRepository emptySubRepo = new WatchlistSubscriptionRepository() {
            @Override public void subscribe(UserId userId, WatchlistId watchlistId) {}
            @Override public void unsubscribe(UserId userId, WatchlistId watchlistId) {}
            @Override public boolean isSubscribed(UserId userId, WatchlistId watchlistId) { return false; }
            @Override public List<WatchlistId> findSubscribedWatchlistIds(UserId userId) { return List.of(); }
            @Override public void deleteByWatchlistId(WatchlistId watchlistId) {}
            @Override public void deleteByUserId(UserId userId) {}
        };
        new UserController(
                getDetails,
                new ExportUserDataUseCase(userRepo, emptyWlRepo, emptySubRepo, mediaRepo, Clock.systemUTC()),
                new UpdateQueryFlavorUseCase(userRepo),
                new DeleteUserUseCase(userRepo, emptyWlRepo, emptySubRepo, noopTx),
                new MarkMediaSeenUseCase(userRepo),
                new UnmarkMediaSeenUseCase(userRepo),
                authController
        ).registerRoutes(app);
        app.get("/api/boom", ctx -> {
            throw new IllegalStateException("secret SQL connection string leaked");
        });

        app.start(0);
        try {
            HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
            String base = "http://localhost:" + app.port();

            // Unauthenticated /api/users/me -> 401
            HttpResponse<String> unauth = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/users/me")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(unauth.statusCode()).isEqualTo(401);

            HttpRequest registerReq = HttpRequest.newBuilder(URI.create(base + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"Nil\",\"password\":\"secret123\"}"))
                    .build();

            HttpResponse<String> registered = client.send(registerReq, HttpResponse.BodyHandlers.ofString());
            assertThat(registered.statusCode()).isEqualTo(201);
            assertThat(registered.headers().firstValue("X-Trace-Id")).isPresent();
            assertThat(registered.headers().firstValue("Cache-Control")).contains("no-store");

            // Duplicate register -> 409
            HttpResponse<String> duplicate = client.send(registerReq, HttpResponse.BodyHandlers.ofString());
            assertThat(duplicate.statusCode()).isEqualTo(409);
            assertThat(duplicate.body()).contains("\"code\":\"CONFLICT\"");

            // Authenticated /api/users/me -> 200
            HttpResponse<String> me = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/users/me")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(me.body()).contains("\"name\":\"Nil\"");
            assertThat(me.body()).contains("\"queryFlavor\":\"NETFLIX_AND_CHILL\"");
            assertThat(me.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
            assertThat(me.headers().firstValue("X-Frame-Options")).contains("DENY");

            HttpResponse<String> prefRes = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/users/me/preferences"))
                            .header("Content-Type", "application/json")
                            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"queryFlavor\":\"CINEPHILE\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(prefRes.statusCode()).isEqualTo(200);
            assertThat(prefRes.body()).contains("\"queryFlavor\":\"CINEPHILE\"");

            // Authenticated /api/users/me/export -> 200 JSON backup without passwordHash
            HttpResponse<String> exportRes = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/users/me/export")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(exportRes.statusCode()).isEqualTo(200);
            assertThat(exportRes.headers().firstValue("Content-Disposition").orElse(""))
                    .contains("attachment; filename=\"cinema-roulette-backup-");
            assertThat(exportRes.headers().firstValue("Cache-Control")).contains("no-store");
            assertThat(exportRes.body()).contains("\"username\":\"Nil\"");
            assertThat(exportRes.body()).doesNotContain("passwordHash");

            // Change password: wrong current password -> 401
            HttpResponse<String> badPwdChange = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/auth/password"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"currentPassword\":\"wrong\",\"newPassword\":\"newsecret123\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(badPwdChange.statusCode()).isEqualTo(401);

            // Change password: valid -> 204 and rotates cookie
            HttpResponse<String> okPwdChange = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/auth/password"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"currentPassword\":\"secret123\",\"newPassword\":\"newsecret123\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(okPwdChange.statusCode()).isEqualTo(204);
            assertThat(okPwdChange.headers().firstValue("Cache-Control")).contains("no-store");

            // Login with new password succeeds
            HttpResponse<String> loginNew = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"Nil\",\"password\":\"newsecret123\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(loginNew.statusCode()).isEqualTo(200);

            HttpRequest badLogin = HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"Nil\",\"password\":\"wrongpass\"}"))
                    .build();
            for (int i = 0; i < 4; i++) {
                HttpResponse<String> attempt = client.send(badLogin, HttpResponse.BodyHandlers.ofString());
                assertThat(attempt.statusCode()).isEqualTo(401);
            }
            HttpResponse<String> throttled = client.send(badLogin, HttpResponse.BodyHandlers.ofString());
            assertThat(throttled.statusCode()).isEqualTo(429);
            assertThat(throttled.headers().firstValue("Retry-After")).isPresent();

            // Sanitized 500
            HttpResponse<String> boom = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/boom")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(boom.statusCode()).isEqualTo(500);
            assertThat(boom.body()).contains("\"code\":\"INTERNAL_ERROR\"");
            assertThat(boom.body()).doesNotContain("secret SQL connection string");
        } finally {
            app.stop();
        }
    }

    private static final class InMemoryUserRepo implements UserRepository {
        private final Map<UserId, User> store = new HashMap<>();
        @Override public List<User> findAll() { return List.copyOf(store.values()); }
        @Override public Map<UserId, String> findNamesByIds(java.util.Set<UserId> ids) { return Map.of(); }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByName(String name) { return store.values().stream().filter(u -> u.name().equals(name)).findFirst(); }
        @Override public User save(User user) { store.put(user.id(), user); return user; }
        @Override public void deleteById(UserId id) { store.remove(id); }
    }

    private static final class EmptyMediaRepo implements MediaItemRepository {
        @Override public void upsertAll(List<MediaItem> items) {}
        @Override public Optional<MediaItem> findById(ImdbId id) { return Optional.empty(); }
        @Override public List<MediaItem> findByIds(Collection<ImdbId> ids) { return List.of(); }
    }
}
