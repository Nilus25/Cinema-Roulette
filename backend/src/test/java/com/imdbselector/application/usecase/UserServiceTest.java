package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.application.usecase.user.DeleteUserUseCase;
import com.imdbselector.application.usecase.user.GetUserDetailsUseCase;
import com.imdbselector.application.usecase.user.MarkMediaSeenUseCase;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
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
    void surfacesUnseenLastGeneratedPromptUntilMarkedAsSeen() {
        FakeMediaRepo mediaRepo = new FakeMediaRepo();
        MediaItem item = new MediaItem(new ImdbId("tt0111161"), "Shawshank", "Darabont",
                null, null, MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV);
        mediaRepo.upsertAll(List.of(item));

        FakeUserRepo userRepo = new FakeUserRepo();
        GetUserDetailsUseCase getDetails = new GetUserDetailsUseCase(userRepo, mediaRepo);
        MarkMediaSeenUseCase markSeen = new MarkMediaSeenUseCase(userRepo);

        User user = User.create("Nil");
        user.recordGenerated(item.imdbId(), Instant.now());
        userRepo.save(user);

        assertThat(getDetails.execute(user.id()).unseenLastGeneratedPrompt()).contains(item);

        markSeen.execute(user.id(), item.imdbId());
        assertThat(getDetails.execute(user.id()).unseenLastGeneratedPrompt()).isEmpty();
    }

    @Test
    void throwsWhenUserNotFound() {
        TransactionPort noopTx = new TransactionPort() {
            @Override public <T> T inTransaction(java.util.function.Supplier<T> work) { return work.get(); }
        };
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
        DeleteUserUseCase deleteUser = new DeleteUserUseCase(new FakeUserRepo(), emptyWlRepo, emptySubRepo, noopTx);
        assertThatThrownBy(() -> deleteUser.execute(UserId.random()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static final class FakeUserRepo implements UserRepository {
        private final Map<UserId, User> store = new HashMap<>();
        @Override public List<User> findAll() { return List.copyOf(store.values()); }
        @Override public Map<UserId, String> findNamesByIds(Set<UserId> ids) { return Map.of(); }
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
