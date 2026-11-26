package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.service.RandomMediaSelector;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MediaSelectionServiceTest {

    @Test
    void selectsUnseenItemAndPersistsLastGeneratedOnUser() {
        var item1 = new MediaItem(new ImdbId("tt0111161"), "Shawshank", "Darabont",
                new ReleaseYear(1994), Rating.of(9.3), MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV);
        var item2 = new MediaItem(new ImdbId("tt0068646"), "Godfather", "Coppola",
                new ReleaseYear(1972), Rating.of(9.2), MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV);

        var user = User.create("Nil");
        user.markAsSeen(item1.imdbId());
        var watchlist = Watchlist.create("Favorites", List.of(item1.imdbId(), item2.imdbId()));

        var userRepo = new InMemoryUserRepo(user);
        var wlRepo = new InMemoryWatchlistRepo(watchlist);
        var mediaRepo = new InMemoryMediaRepo(List.of(item1, item2));

        var fixedNow = Instant.parse("2026-11-26T12:00:00Z");
        var service = new MediaSelectionService(
                wlRepo, mediaRepo, userRepo,
                new WatchlistFilterService(),
                new RandomMediaSelector(new Random(1)),
                Clock.fixed(fixedNow, ZoneOffset.UTC));

        var criteria = new FilterCriteria(null, null, null, null, Set.of(), Set.of(), List.of(), true);
        MediaItem picked = service.selectRandom(watchlist.id(), user.id(), criteria);

        assertThat(picked).isEqualTo(item2);
        assertThat(user.lastGeneratedImdbId()).isEqualTo(item2.imdbId());
        assertThat(user.lastGeneratedAt()).isEqualTo(fixedNow);
    }

    private static final class InMemoryUserRepo implements UserRepository {
        private final Map<UserId, User> store = new HashMap<>();
        InMemoryUserRepo(User... users) { for (User u : users) store.put(u.id(), u); }
        @Override public List<User> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByName(String name) { return store.values().stream().filter(u -> u.name().equals(name)).findFirst(); }
        @Override public User save(User user) { store.put(user.id(), user); return user; }
        @Override public void deleteById(UserId id) { store.remove(id); }
    }

    private static final class InMemoryWatchlistRepo implements WatchlistRepository {
        private final Map<WatchlistId, Watchlist> store = new HashMap<>();
        InMemoryWatchlistRepo(Watchlist... lists) { for (Watchlist w : lists) store.put(w.id(), w); }
        @Override public List<Watchlist> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<Watchlist> findById(WatchlistId id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Watchlist> findByName(String name) { return store.values().stream().filter(w -> w.name().equals(name)).findFirst(); }
        @Override public Watchlist save(Watchlist watchlist) { store.put(watchlist.id(), watchlist); return watchlist; }
        @Override public void deleteById(WatchlistId id) { store.remove(id); }
    }

    private static final class InMemoryMediaRepo implements MediaItemRepository {
        private final Map<ImdbId, MediaItem> store = new HashMap<>();
        InMemoryMediaRepo(List<MediaItem> items) { upsertAll(items); }
        @Override public void upsertAll(List<MediaItem> items) { for (MediaItem m : items) store.put(m.imdbId(), m); }
        @Override public Optional<MediaItem> findById(ImdbId id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<MediaItem> findByIds(Collection<ImdbId> ids) { return ids.stream().map(store::get).filter(Objects::nonNull).toList(); }
    }
}
