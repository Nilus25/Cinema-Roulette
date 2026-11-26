package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ListAccessibleWatchlistsUseCase {
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public ListAccessibleWatchlistsUseCase(
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            UserRepository userRepository
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public List<WatchlistSummary> execute(UserId userId) {
        List<Watchlist> owned = watchlistRepository.findByOwner(userId);
        List<WatchlistId> subIds = subscriptionRepository.findSubscribedWatchlistIds(userId);
        List<Watchlist> subscribed = watchlistRepository.findByIds(subIds);
        List<Watchlist> combined = new ArrayList<>(owned.size() + subscribed.size());
        combined.addAll(owned);
        combined.addAll(subscribed);
        Map<UserId, String> ownerNames = WatchlistAccess.ownerNamesMap(userRepository, combined);

        List<WatchlistSummary> result = new ArrayList<>();
        for (Watchlist w : owned) {
            result.add(new WatchlistSummary(w, true, ownerNames.getOrDefault(w.ownerUserId(), "You")));
        }
        for (Watchlist w : subscribed) {
            if (!w.isOwnedBy(userId)) {
                result.add(new WatchlistSummary(w, false, ownerNames.getOrDefault(w.ownerUserId(), "Unknown")));
            }
        }
        return result;
    }
}
