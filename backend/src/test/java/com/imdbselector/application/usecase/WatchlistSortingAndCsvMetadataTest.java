package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.application.usecase.watchlist.GetWatchlistItemsUseCase;
import com.imdbselector.application.usecase.watchlist.WatchlistItemView;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.SortBy;
import com.imdbselector.domain.valueobject.SortDirection;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.infrastructure.adapter.out.csv.OpenCsvWatchlistParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistSortingAndCsvMetadataTest {

    @Test
    void parsesRuntimeNumVotesAndOriginalTitleFromImdbCsv() {
        String csv = """
                Position,Const,Created,Modified,Description,Title,Original Title,URL,Title Type,IMDb Rating,Runtime (mins),Year,Genres,Num Votes,Release Date,Directors,Your Rating,Date Rated
                1,tt0060196,2019-12-27,2019-12-27,,"The Good, the Bad and the Ugly","Il buono, il brutto, il cattivo",https://www.imdb.com/title/tt0060196/,Movie,8.8,178,1966,"Adventure, Drama, Western",907647,1967-12-29,"Sergio Leone",,
                2,tt9999999,2020-01-01,2020-01-01,,"Mystery Short","",https://www.imdb.com/title/tt9999999/,Short,,,,Drama,,,,,
                """;
        OpenCsvWatchlistParser parser = new OpenCsvWatchlistParser();
        List<MediaItem> parsed = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed).hasSize(2);
        MediaItem classic = parsed.get(0);
        assertThat(classic.originalTitle()).isEqualTo("Il buono, il brutto, il cattivo");
        assertThat(classic.runtimeMinutes()).isEqualTo(178);
        assertThat(classic.numVotes()).isEqualTo(907647);

        MediaItem sparse = parsed.get(1);
        assertThat(sparse.originalTitle()).isNull();
        assertThat(sparse.runtimeMinutes()).isNull();
        assertThat(sparse.numVotes()).isNull();
    }

    @Test
    void sortsItemsByRuntimeVotesAndUnseenWithNullsLast() {
        User user = User.register("Nil", "hash");
        MediaItem longMovie = new MediaItem(
                new ImdbId("tt0000001"), "Alpha", null, "Dir A",
                new ReleaseYear(2000), Rating.of(8.5), 180, 500000,
                MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV
        );
        MediaItem shortMovie = new MediaItem(
                new ImdbId("tt0000002"), "Beta", null, "Dir B",
                new ReleaseYear(2020), Rating.of(7.5), 85, 120000,
                MediaType.MOVIE, null, Set.of(Genre.COMEDY), MetadataSource.CSV
        );
        MediaItem unknownRuntime = new MediaItem(
                new ImdbId("tt0000003"), "Gamma", null, "Dir C",
                new ReleaseYear(1995), Rating.of(9.0), null, null,
                MediaType.MOVIE, null, Set.of(Genre.DRAMA), MetadataSource.CSV
        );
        user.markAsSeen(longMovie.imdbId());

        Watchlist watchlist = Watchlist.create(
                user.id(),
                "Main",
                List.of(longMovie.imdbId(), shortMovie.imdbId(), unknownRuntime.imdbId())
        );

        GetWatchlistItemsUseCase useCase = new GetWatchlistItemsUseCase(
                singleWatchlistRepo(watchlist),
                emptySubscriptionRepo(),
                inMemoryMediaRepo(List.of(longMovie, shortMovie, unknownRuntime)),
                singleUserRepo(user)
        );

        List<WatchlistItemView> byRuntimeAsc = useCase.execute(
                user.id(), watchlist.id(), FilterCriteria.unfiltered(), SortBy.RUNTIME, SortDirection.ASC);
        assertThat(byRuntimeAsc).extracting(v -> v.item().title())
                .containsExactly("Beta", "Alpha", "Gamma");

        List<WatchlistItemView> byVotesDesc = useCase.execute(
                user.id(), watchlist.id(), FilterCriteria.unfiltered(), SortBy.VOTES, SortDirection.DESC);
        assertThat(byVotesDesc).extracting(v -> v.item().title())
                .containsExactly("Alpha", "Beta", "Gamma");

        List<WatchlistItemView> unseenFirst = useCase.execute(
                user.id(), watchlist.id(), FilterCriteria.unfiltered(), SortBy.UNSEEN, SortDirection.DESC);
        assertThat(unseenFirst).extracting(v -> v.item().title())
                .containsExactly("Beta", "Gamma", "Alpha");
    }

    private static WatchlistRepository singleWatchlistRepo(Watchlist wl) {
        return new WatchlistRepository() {
            @Override public List<Watchlist> findByOwner(UserId ownerId) { return List.of(wl); }
            @Override public List<Watchlist> findByIds(Collection<WatchlistId> ids) { return List.of(wl); }
            @Override public List<Watchlist> findPublicWatchlists() { return List.of(); }
            @Override public Optional<Watchlist> findById(WatchlistId id) { return Optional.of(wl); }
            @Override public Optional<Watchlist> findByOwnerAndName(UserId ownerId, String name) { return Optional.of(wl); }
            @Override public Optional<Watchlist> findByShareCode(String shareCode) { return Optional.empty(); }
            @Override public Watchlist save(Watchlist watchlist) { return watchlist; }
            @Override public void deleteById(WatchlistId id) {}
        };
    }

    private static WatchlistSubscriptionRepository emptySubscriptionRepo() {
        return new WatchlistSubscriptionRepository() {
            @Override public void subscribe(UserId userId, WatchlistId watchlistId) {}
            @Override public void unsubscribe(UserId userId, WatchlistId watchlistId) {}
            @Override public boolean isSubscribed(UserId userId, WatchlistId watchlistId) { return false; }
            @Override public List<WatchlistId> findSubscribedWatchlistIds(UserId userId) { return List.of(); }
            @Override public void deleteByWatchlistId(WatchlistId watchlistId) {}
            @Override public void deleteByUserId(UserId userId) {}
        };
    }

    private static MediaItemRepository inMemoryMediaRepo(List<MediaItem> items) {
        return new MediaItemRepository() {
            @Override public void upsertAll(List<MediaItem> list) {}
            @Override public Optional<MediaItem> findById(ImdbId id) {
                return items.stream().filter(i -> i.imdbId().equals(id)).findFirst();
            }
            @Override public List<MediaItem> findByIds(Collection<ImdbId> ids) { return items; }
        };
    }

    private static UserRepository singleUserRepo(User user) {
        return new UserRepository() {
            @Override public List<User> findAll() { return List.of(user); }
            @Override public Map<UserId, String> findNamesByIds(Set<UserId> ids) { return Map.of(user.id(), user.name()); }
            @Override public Optional<User> findById(UserId id) { return Optional.of(user); }
            @Override public Optional<User> findByName(String name) { return Optional.of(user); }
            @Override public User save(User u) { return u; }
            @Override public void deleteById(UserId id) {}
        };
    }
}
