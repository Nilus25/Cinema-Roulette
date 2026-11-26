package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    @Test
    void rejectsDuplicateUserName() {
        var service = new UserService(new FakeUserRepo(), new FakeMediaRepo());
        service.createUser("Nil");

        assertThatThrownBy(() -> service.createUser("Nil"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void surfacesUnseenLastGeneratedPromptUntilMarkedAsSeen() {
        var mediaRepo = new FakeMediaRepo();
        var item = new MediaItem(new ImdbId("tt0111161"), "Shawshank", "Darabont",
                null, null, MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV);
        mediaRepo.upsertAll(List.of(item));

        var userRepo = new FakeUserRepo();
        var service = new UserService(userRepo, mediaRepo);
        User user = service.createUser("Nil");
        user.recordGenerated(item.imdbId(), Instant.now());
        userRepo.save(user);

        assertThat(service.getUserDetails(user.id()).unseenLastGeneratedPrompt()).contains(item);

        service.markAsSeen(user.id(), item.imdbId());
        assertThat(service.getUserDetails(user.id()).unseenLastGeneratedPrompt()).isEmpty();
    }

    @Test
    void throwsWhenUserNotFound() {
        var service = new UserService(new FakeUserRepo(), new FakeMediaRepo());
        assertThatThrownBy(() -> service.deleteUser(UserId.random()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static final class FakeUserRepo implements UserRepository {
        private final Map<UserId, User> store = new HashMap<>();
        @Override public List<User> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByName(String name) { return store.values().stream().filter(u -> u.name().equals(name)).findFirst(); }
        @Override public User save(User user) { store.put(user.id(), user); return user; }
        @Override public void deleteById(UserId id) { store.remove(id); }
    }

    private static final class FakeMediaRepo implements MediaItemRepository {
        private final Map<ImdbId, MediaItem> store = new HashMap<>();
        @Override public void upsertAll(List<MediaItem> items) { for (MediaItem m : items) store.put(m.imdbId(), m); }
        @Override public Optional<MediaItem> findById(ImdbId id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<MediaItem> findByIds(Collection<ImdbId> ids) { return ids.stream().map(store::get).filter(Objects::nonNull).toList(); }
    }
}
