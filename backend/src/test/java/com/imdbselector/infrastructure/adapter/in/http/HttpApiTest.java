package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.usecase.UserService;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HttpApiTest {

    @Test
    void createsUsersAndMapsDuplicateTo409() throws Exception {
        var userService = new UserService(new InMemoryUserRepo(), new EmptyMediaRepo());
        Javalin app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json");
        new UserController(userService).registerRoutes(app);
        app.exception(DuplicateResourceException.class, (e, ctx) ->
                ctx.status(409).json(new ApiDtos.ErrorResponse("CONFLICT", e.getMessage())));

        app.start(0);
        try {
            HttpClient client = HttpClient.newHttpClient();
            String base = "http://localhost:" + app.port();

            var createReq = HttpRequest.newBuilder(URI.create(base + "/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Nil\"}"))
                    .build();

            HttpResponse<String> first = client.send(createReq, HttpResponse.BodyHandlers.ofString());
            assertThat(first.statusCode()).isEqualTo(201);
            assertThat(first.body()).contains("\"name\":\"Nil\"");

            HttpResponse<String> duplicate = client.send(createReq, HttpResponse.BodyHandlers.ofString());
            assertThat(duplicate.statusCode()).isEqualTo(409);
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
