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
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

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
        Watchlist watchlist = WatchlistAccess.findViewableOrThrow(
                watchlistRepository, subscriptionRepository, actorId, watchlistId);
        List<MediaItem> items = mediaItemRepository.findByIds(watchlist.mediaItemIds());
        List<MediaItem> filtered = Watchlist.filterItems(items, criteria);

        Set<ImdbId> seenIds = userRepository.findById(actorId)
                .map(User::seenMediaIds)
                .orElse(Set.of());

        return filtered.stream()
                .filter(item -> criteria == null || !criteria.excludeSeen() || !seenIds.contains(item.imdbId()))
                .map(item -> new WatchlistItemView(item, seenIds.contains(item.imdbId())))
                .toList();
    }
}
