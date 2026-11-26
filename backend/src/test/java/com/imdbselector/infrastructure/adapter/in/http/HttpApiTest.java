package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.usecase.AuthService;
import com.imdbselector.application.usecase.UserService;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
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
        var userRepo = new InMemoryUserRepo();
        var mediaRepo = new EmptyMediaRepo();
        var authService = new AuthService(
                userRepo,
                new BcryptPasswordHasher(),
                new JwtTokenAdapter("test-secret-at-least-32-chars-long!", Clock.systemUTC(), Duration.ofHours(1))
        );
        var userService = new UserService(userRepo, mediaRepo);

        Javalin app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json");
        HttpObservability.register(app);
        var authController = new AuthController(authService, userService);
        authController.registerRoutes(app);
        new UserController(userService, authController).registerRoutes(app);
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

            var registerReq = HttpRequest.newBuilder(URI.create(base + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"Nil\",\"password\":\"secret123\"}"))
                    .build();

            HttpResponse<String> registered = client.send(registerReq, HttpResponse.BodyHandlers.ofString());
            assertThat(registered.statusCode()).isEqualTo(201);
            assertThat(registered.headers().firstValue("X-Trace-Id")).isPresent();

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
