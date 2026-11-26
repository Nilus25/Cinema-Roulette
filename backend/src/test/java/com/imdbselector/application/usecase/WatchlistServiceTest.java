package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.application.usecase.watchlist.ChangeWatchlistVisibilityUseCase;
import com.imdbselector.application.usecase.watchlist.GetWatchlistItemsUseCase;
import com.imdbselector.application.usecase.watchlist.ImportWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.PreviewWatchlistUpdateUseCase;
import com.imdbselector.application.usecase.watchlist.SubscribeWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.UpdateWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.WatchlistItemView;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistDiff;
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
        FakeWatchlistRepo wlRepo = new FakeWatchlistRepo();
        FakeSubRepo subRepo = new FakeSubRepo();
        FakeMediaRepo mediaRepo = new FakeMediaRepo();
        FakeUserRepo userRepo = new FakeUserRepo();
        User owner = User.create("Nil");
        User friend = User.create("Alex");
        userRepo.save(owner);
        userRepo.save(friend);

        Clock clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);
        OpenCsvWatchlistParser csvParser = new OpenCsvWatchlistParser();
        TransactionPort noopTx = new TransactionPort() {
            @Override public <T> T inTransaction(java.util.function.Supplier<T> work) { return work.get(); }
        };
        ImportWatchlistUseCase importWatchlist = new ImportWatchlistUseCase(wlRepo, mediaRepo, csvParser, noopTx);
        PreviewWatchlistUpdateUseCase previewUpdate = new PreviewWatchlistUpdateUseCase(wlRepo, mediaRepo, csvParser);
        UpdateWatchlistUseCase updateWatchlist = new UpdateWatchlistUseCase(wlRepo, mediaRepo, userRepo, csvParser, noopTx, clock);
        ChangeWatchlistVisibilityUseCase changeVisibility = new ChangeWatchlistVisibilityUseCase(wlRepo, clock);
        SubscribeWatchlistUseCase subscribeWatchlist = new SubscribeWatchlistUseCase(wlRepo, subRepo);
        GetWatchlistItemsUseCase getWatchlistItems = new GetWatchlistItemsUseCase(wlRepo, subRepo, mediaRepo, userRepo);

        Watchlist created = importWatchlist.execute(owner.id(), "Classics", new ByteArrayInputStream(CSV_V1.getBytes(StandardCharsets.UTF_8)));
        assertThat(created.mediaItemIds()).containsExactly(new ImdbId("tt0111161"));

        assertThatThrownBy(() -> importWatchlist.execute(owner.id(), "Classics", new ByteArrayInputStream(CSV_V1.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(DuplicateResourceException.class);

        assertThatThrownBy(() -> updateWatchlist.execute(friend.id(), created.id(), new ByteArrayInputStream(CSV_V2.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ForbiddenException.class);

        Watchlist updated = updateWatchlist.execute(owner.id(), created.id(), new ByteArrayInputStream(CSV_V2.getBytes(StandardCharsets.UTF_8)));
        assertThat(updated.mediaItemIds()).containsExactly(new ImdbId("tt0068646"));

        Watchlist shared = changeVisibility.execute(owner.id(), created.id(), WatchlistVisibility.SHARED);
        assertThat(shared.shareCode()).isNotBlank();

        subscribeWatchlist.byShareCode(friend.id(), shared.shareCode());
        List<WatchlistItemView> friendItems = getWatchlistItems.execute(friend.id(), created.id(), FilterCriteria.unfiltered());
        assertThat(friendItems).hasSize(1);
        assertThat(friendItems.get(0).item().title()).isEqualTo("The Godfather");

        WatchlistDiff diff = previewUpdate.execute(owner.id(), created.id(), new ByteArrayInputStream(CSV_V1.getBytes(StandardCharsets.UTF_8)));
        assertThat(diff.added()).extracting(MediaItem::imdbId).containsExactly(new ImdbId("tt0111161"));
        assertThat(diff.removed()).extracting(MediaItem::imdbId).containsExactly(new ImdbId("tt0068646"));

        Watchlist selective = updateWatchlist.execute(
                owner.id(),
                created.id(),
                new ByteArrayInputStream(CSV_V1.getBytes(StandardCharsets.UTF_8)),
                Set.of(),
                Set.of(new ImdbId("tt0068646")),
                Set.of(new ImdbId("tt0111161"))
        );
        assertThat(selective.mediaItemIds()).containsExactlyInAnyOrder(new ImdbId("tt0068646"), new ImdbId("tt0111161"));
        assertThat(userRepo.findById(owner.id()).orElseThrow().seenMediaIds()).contains(new ImdbId("tt0111161"));
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
        @Override public void deleteByWatchlistId(WatchlistId w) { subs.values().forEach(s -> s.remove(w)); }
        @Override public void deleteByUserId(UserId u) { subs.remove(u); }
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
        @Override public Map<UserId, String> findNamesByIds(Set<UserId> ids) {
            Map<UserId, String> out = new HashMap<>();
            for (UserId id : ids) {
                User u = store.get(id);
                if (u != null) out.put(id, u.name());
            }
            return out;
        }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByName(String name) { return store.values().stream().filter(u -> u.name().equals(name)).findFirst(); }
        @Override public User save(User user) { store.put(user.id(), user); return user; }
        @Override public void deleteById(UserId id) { store.remove(id); }
    }
}
