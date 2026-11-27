package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.SortBy;
import com.imdbselector.domain.valueobject.SortDirection;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GetWatchlistItemsUseCase {
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;

    public GetWatchlistItemsUseCase(
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            MediaItemRepository mediaItemRepository,
            UserRepository userRepository
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public List<WatchlistItemView> execute(UserId actorId, WatchlistId watchlistId, FilterCriteria criteria) {
        return execute(actorId, watchlistId, criteria, SortBy.DEFAULT, SortDirection.DESC);
    }

    public List<WatchlistItemView> execute(
            UserId actorId,
            WatchlistId watchlistId,
            FilterCriteria criteria,
            SortBy sortBy,
            SortDirection sortDirection
    ) {
        Watchlist watchlist = WatchlistAccess.findViewableOrThrow(
                watchlistRepository, subscriptionRepository, actorId, watchlistId);
        List<MediaItem> items = mediaItemRepository.findByIds(watchlist.mediaItemIds());
        Set<ImdbId> seenIds = userRepository.findById(actorId)
                .map(User::seenMediaIds)
                .orElse(Set.of());

        List<MediaItem> filtered = Watchlist.filterItems(items, criteria, seenIds);

        List<WatchlistItemView> views = filtered.stream()
                .filter(item -> criteria == null || !criteria.excludeSeen() || !seenIds.contains(item.imdbId()))
                .map(item -> new WatchlistItemView(item, seenIds.contains(item.imdbId())))
                .toList();

        SortBy effectiveSort = sortBy == null ? SortBy.DEFAULT : sortBy;
        if (effectiveSort == SortBy.DEFAULT) {
            return views;
        }
        SortDirection effectiveDir = sortDirection == null ? defaultDirection(effectiveSort) : sortDirection;
        Comparator<WatchlistItemView> comparator = buildComparator(effectiveSort, effectiveDir);
        return views.stream().sorted(comparator).toList();
    }

    private static SortDirection defaultDirection(SortBy sortBy) {
        return switch (sortBy) {
            case TITLE -> SortDirection.ASC;
            case DEFAULT, RATING, YEAR, RUNTIME, VOTES, UNSEEN -> SortDirection.DESC;
        };
    }

    private static Comparator<WatchlistItemView> buildComparator(SortBy sortBy, SortDirection direction) {
        Comparator<WatchlistItemView> tieBreaker = Comparator.comparing(
                v -> v.item().title(), String.CASE_INSENSITIVE_ORDER);

        Comparator<WatchlistItemView> primary = switch (sortBy) {
            case DEFAULT -> (a, b) -> 0;
            case RATING -> Comparator.comparing(
                    v -> v.item().rating() == null ? null : v.item().rating().value(),
                    nullsLastComparator(BigDecimal::compareTo, direction)
            );
            case YEAR -> Comparator.comparing(
                    v -> v.item().releaseYear() == null ? null : v.item().releaseYear().value(),
                    nullsLastComparator(Integer::compareTo, direction)
            );
            case RUNTIME -> Comparator.comparing(
                    v -> v.item().runtimeMinutes(),
                    nullsLastComparator(Integer::compareTo, direction)
            );
            case VOTES -> Comparator.comparing(
                    v -> v.item().numVotes(),
                    nullsLastComparator(Integer::compareTo, direction)
            );
            case TITLE -> direction == SortDirection.ASC
                    ? Comparator.comparing(v -> v.item().title(), String.CASE_INSENSITIVE_ORDER)
                    : Comparator.comparing(v -> v.item().title(), String.CASE_INSENSITIVE_ORDER.reversed());
            case UNSEEN -> direction == SortDirection.DESC
                    ? Comparator.comparing(WatchlistItemView::seen)
                    : Comparator.comparing(WatchlistItemView::seen).reversed();
        };
        return primary.thenComparing(tieBreaker);
    }

    private static <T> Comparator<T> nullsLastComparator(Comparator<T> natural, SortDirection direction) {
        Comparator<T> ordered = direction == SortDirection.ASC ? natural : natural.reversed();
        return Comparator.nullsLast(ordered);
    }
}
