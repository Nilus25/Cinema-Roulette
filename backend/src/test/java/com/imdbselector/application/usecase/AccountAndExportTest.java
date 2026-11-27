package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.application.usecase.auth.AuthSession;
import com.imdbselector.application.usecase.auth.ChangePasswordUseCase;
import com.imdbselector.application.usecase.user.ExportUserDataUseCase;
import com.imdbselector.application.usecase.user.UserExportData;
import com.imdbselector.domain.exception.UnauthorizedException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;
import com.imdbselector.infrastructure.adapter.out.security.BcryptPasswordHasher;
import com.imdbselector.infrastructure.adapter.out.security.JwtTokenAdapter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountAndExportTest {

    @Test
    void changesPasswordWhenCurrentMatchesAndRejectsInvalidCases() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        BcryptPasswordHasher hasher = new BcryptPasswordHasher();
        JwtTokenAdapter tokenPort = new JwtTokenAdapter(
                "test-secret-at-least-32-chars-long!", Clock.systemUTC(), Duration.ofHours(1));
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(userRepo, hasher, tokenPort);

        User user = User.register("Nil", hasher.hash("oldpass123"));
        userRepo.save(user);

        assertThatThrownBy(() -> useCase.execute(user.id(), "wrongpass", "newpass123"))
                .isInstanceOf(UnauthorizedException.class);

        assertThatThrownBy(() -> useCase.execute(user.id(), "oldpass123", "oldpass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        assertThatThrownBy(() -> useCase.execute(user.id(), "oldpass123", "short"))
                .isInstanceOf(IllegalArgumentException.class);

        AuthSession session = useCase.execute(user.id(), "oldpass123", "newpass123");
        assertThat(session.token()).isNotBlank();
        assertThat(hasher.matches("newpass123", userRepo.findById(user.id()).orElseThrow().passwordHash())).isTrue();
    }

    @Test
    void exportsSeenMoviesOwnedWatchlistsAndSubscriptionSummaries() {
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        InMemoryMediaRepo mediaRepo = new InMemoryMediaRepo();
        InMemoryWatchlistRepo watchlistRepo = new InMemoryWatchlistRepo();
        InMemorySubRepo subRepo = new InMemorySubRepo();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-27T12:00:00Z"), ZoneOffset.UTC);

        User nil = User.register("Nil", "hash");
        User friend = User.register("Alex", "hash");
        userRepo.save(nil);
        userRepo.save(friend);

        MediaItem m1 = new MediaItem(new ImdbId("tt0111161"), "Shawshank", "Darabont",
                null, null, MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV);
        MediaItem m2 = new MediaItem(new ImdbId("tt0468569"), "The Dark Knight", "Nolan",
                null, null, MediaType.MOVIE, null, Set.of(Genre.ACTION), MetadataSource.CSV);
        MediaItem m3 = new MediaItem(new ImdbId("tt1375666"), "Inception", "Nolan",
                null, null, MediaType.MOVIE, null, Set.of(Genre.SCI_FI), MetadataSource.CSV);
        mediaRepo.upsertAll(List.of(m1, m2, m3));

        nil.markAsSeen(m1.imdbId());
        userRepo.save(nil);

        Watchlist owned = Watchlist.create(nil.id(), "My Favorites", List.of(m1.imdbId(), m2.imdbId()));
        Watchlist friendShared = Watchlist.create(friend.id(), "Alex Picks", List.of(m3.imdbId()));
        friendShared.changeVisibility(WatchlistVisibility.SHARED, "SHARE123", fixedClock.instant());
        watchlistRepo.save(owned);
        watchlistRepo.save(friendShared);
        subRepo.subscribe(nil.id(), friendShared.id());

        ExportUserDataUseCase useCase = new ExportUserDataUseCase(userRepo, watchlistRepo, subRepo, mediaRepo, fixedClock);
        UserExportData result = useCase.execute(nil.id());

        assertThat(result.exportedAt()).isEqualTo(fixedClock.instant());
        assertThat(result.seenMovies()).containsExactly(m1);
        assertThat(result.ownedWatchlists()).hasSize(1);
        assertThat(result.ownedWatchlists().get(0).items()).containsExactly(m1, m2);
        assertThat(result.subscriptions()).hasSize(1);
        assertThat(result.subscriptions().get(0).watchlist().name()).isEqualTo("Alex Picks");
        assertThat(result.subscriptions().get(0).ownerName()).isEqualTo("Alex");
    }

    private static final class InMemoryUserRepo implements UserRepository {
        private final Map<UserId, User> store = new HashMap<>();

        @Override public List<User> findAll() { return List.copyOf(store.values()); }
        @Override public Map<UserId, String> findNamesByIds(Set<UserId> ids) {
            return ids.stream()
                    .filter(store::containsKey)
                    .collect(Collectors.toMap(id -> id, id -> store.get(id).name()));
        }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByName(String name) {
            return store.values().stream().filter(u -> u.name().equals(name)).findFirst();
        }
        @Override public User save(User user) { store.put(user.id(), user); return user; }
        @Override public void deleteById(UserId id) { store.remove(id); }
    }

    private static final class InMemoryMediaRepo implements MediaItemRepository {
        private final Map<ImdbId, MediaItem> store = new HashMap<>();

        @Override public void upsertAll(List<MediaItem> items) {
            for (MediaItem item : items) {
                store.put(item.imdbId(), item);
            }
        }
        @Override public Optional<MediaItem> findById(ImdbId id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<MediaItem> findByIds(Collection<ImdbId> ids) {
            return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
        }
    }

    private static final class InMemoryWatchlistRepo implements WatchlistRepository {
        private final Map<WatchlistId, Watchlist> store = new HashMap<>();

        @Override public List<Watchlist> findByOwner(UserId ownerId) {
            return store.values().stream().filter(w -> w.isOwnedBy(ownerId)).toList();
        }
        @Override public List<Watchlist> findByIds(Collection<WatchlistId> ids) {
            return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
        }
        @Override public List<Watchlist> findPublicWatchlists() { return List.of(); }
        @Override public Optional<Watchlist> findById(WatchlistId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Watchlist> findByOwnerAndName(UserId ownerId, String name) { return Optional.empty(); }
        @Override public Optional<Watchlist> findByShareCode(String shareCode) { return Optional.empty(); }
        @Override public Watchlist save(Watchlist watchlist) { store.put(watchlist.id(), watchlist); return watchlist; }
        @Override public void deleteById(WatchlistId id) { store.remove(id); }
    }

    private static final class InMemorySubRepo implements WatchlistSubscriptionRepository {
        private final Map<UserId, List<WatchlistId>> subs = new HashMap<>();

        @Override public void subscribe(UserId userId, WatchlistId watchlistId) {
            subs.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(watchlistId);
        }
        @Override public void unsubscribe(UserId userId, WatchlistId watchlistId) {}
        @Override public boolean isSubscribed(UserId userId, WatchlistId watchlistId) { return false; }
        @Override public List<WatchlistId> findSubscribedWatchlistIds(UserId userId) {
            return subs.getOrDefault(userId, List.of());
        }
        @Override public void deleteByWatchlistId(WatchlistId watchlistId) {}
        @Override public void deleteByUserId(UserId userId) {}
    }
}
