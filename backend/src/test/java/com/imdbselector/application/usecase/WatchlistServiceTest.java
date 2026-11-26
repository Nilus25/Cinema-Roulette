package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;
import com.imdbselector.infrastructure.adapter.out.csv.OpenCsvWatchlistParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatchlistServiceTest {

    private static final String CSV_V1 = """
            Const,Title,URL,Title Type,IMDb Rating,Year,Genres,Directors
            tt0111161,The Shawshank Redemption,https://www.imdb.com/title/tt0111161/,Movie,9.3,1994,Drama,Frank Darabont
            """;

    private static final String CSV_V2 = """
            Const,Title,URL,Title Type,IMDb Rating,Year,Genres,Directors
            tt0068646,The Godfather,https://www.imdb.com/title/tt0068646/,Movie,9.2,1972,"Crime, Drama",Francis Ford Coppola
            """;

    @Test
    void enforcesOwnershipAndSupportsSubscriptions() {
        var wlRepo = new FakeWatchlistRepo();
        var subRepo = new FakeSubRepo();
        var mediaRepo = new FakeMediaRepo();
        var userRepo = new FakeUserRepo();
        var owner = User.create("Nil");
        var friend = User.create("Alex");
        userRepo.save(owner);
        userRepo.save(friend);

        var service = new WatchlistService(
                wlRepo, subRepo, mediaRepo, userRepo,
                new OpenCsvWatchlistParser(),
                new WatchlistFilterService(),
                Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC));

        Watchlist created = service.importFromCsv(owner.id(), "Classics", new ByteArrayInputStream(CSV_V1.getBytes(StandardCharsets.UTF_8)));
        assertThat(created.mediaItemIds()).containsExactly(new ImdbId("tt0111161"));

        assertThatThrownBy(() -> service.importFromCsv(owner.id(), "Classics", new ByteArrayInputStream(CSV_V1.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(DuplicateResourceException.class);

        // Non-owner cannot update or delete
        assertThatThrownBy(() -> service.updateFromCsv(friend.id(), created.id(), new ByteArrayInputStream(CSV_V2.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ForbiddenException.class);

        // Owner updates CSV and shares via code
        Watchlist updated = service.updateFromCsv(owner.id(), created.id(), new ByteArrayInputStream(CSV_V2.getBytes(StandardCharsets.UTF_8)));
        assertThat(updated.mediaItemIds()).containsExactly(new ImdbId("tt0068646"));

        Watchlist shared = service.changeVisibility(owner.id(), created.id(), WatchlistVisibility.SHARED);
        assertThat(shared.shareCode()).isNotBlank();

        service.subscribeByShareCode(friend.id(), shared.shareCode());
        var friendItems = service.getWatchlistItems(friend.id(), created.id(), FilterCriteria.unfiltered());
        assertThat(friendItems).hasSize(1);
        assertThat(friendItems.get(0).item().title()).isEqualTo("The Godfather");
    }

    private static final class FakeWatchlistRepo implements WatchlistRepository {
        private final Map<WatchlistId, Watchlist> store = new HashMap<>();
        @Override public List<Watchlist> findByOwner(UserId owner) { return store.values().stream().filter(w -> w.isOwnedBy(owner)).toList(); }
        @Override public List<Watchlist> findByIds(Collection<WatchlistId> ids) { return ids.stream().map(store::get).filter(Objects::nonNull).toList(); }
        @Override public List<Watchlist> findPublicWatchlists() { return store.values().stream().filter(w -> w.visibility() == WatchlistVisibility.PUBLIC).toList(); }
        @Override public Optional<Watchlist> findById(WatchlistId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Watchlist> findByOwnerAndName(UserId owner, String name) { return store.values().stream().filter(w -> w.isOwnedBy(owner) && w.name().equals(name)).findFirst(); }
        @Override public Optional<Watchlist> findByShareCode(String code) { return store.values().stream().filter(w -> code.equals(w.shareCode())).findFirst(); }
        @Override public Watchlist save(Watchlist wl) { store.put(wl.id(), wl); return wl; }
        @Override public void deleteById(WatchlistId id) { store.remove(id); }
    }

    private static final class FakeSubRepo implements WatchlistSubscriptionRepository {
        private final Map<UserId, Set<WatchlistId>> subs = new HashMap<>();
        @Override public void subscribe(UserId u, WatchlistId w) { subs.computeIfAbsent(u, k -> new HashSet<>()).add(w); }
        @Override public void unsubscribe(UserId u, WatchlistId w) { subs.getOrDefault(u, Set.of()).remove(w); }
        @Override public boolean isSubscribed(UserId u, WatchlistId w) { return subs.getOrDefault(u, Set.of()).contains(w); }
        @Override public List<WatchlistId> findSubscribedWatchlistIds(UserId u) { return List.copyOf(subs.getOrDefault(u, Set.of())); }
    }

    private static final class FakeMediaRepo implements MediaItemRepository {
        private final Map<ImdbId, MediaItem> store = new HashMap<>();
        @Override public void upsertAll(List<MediaItem> items) { for (MediaItem m : items) store.put(m.imdbId(), m); }
        @Override public Optional<MediaItem> findById(ImdbId id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<MediaItem> findByIds(Collection<ImdbId> ids) { return ids.stream().map(store::get).filter(Objects::nonNull).toList(); }
    }

    private static final class FakeUserRepo implements UserRepository {
        private final Map<UserId, User> store = new HashMap<>();
        @Override public List<User> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByName(String name) { return store.values().stream().filter(u -> u.name().equals(name)).findFirst(); }
        @Override public User save(User user) { store.put(user.id(), user); return user; }
        @Override public void deleteById(UserId id) { store.remove(id); }
    }
}
