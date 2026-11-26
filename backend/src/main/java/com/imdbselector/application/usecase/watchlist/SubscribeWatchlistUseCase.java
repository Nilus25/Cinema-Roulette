package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;

import java.util.Objects;

public final class SubscribeWatchlistUseCase {
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;

    public SubscribeWatchlistUseCase(
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
    }

    public void byShareCode(UserId userId, String shareCode) {
        if (shareCode == null || shareCode.isBlank()) {
            throw new IllegalArgumentException("Share code must not be blank");
        }
        Watchlist watchlist = watchlistRepository.findByShareCode(shareCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No watchlist found for share code"));
        if (watchlist.isOwnedBy(userId)) {
            throw new IllegalArgumentException("You already own this watchlist");
        }
        if (watchlist.visibility() == WatchlistVisibility.PRIVATE) {
            throw new ForbiddenException("This watchlist is private");
        }
        subscriptionRepository.subscribe(userId, watchlist.id());
    }

    public void toPublic(UserId userId, WatchlistId watchlistId) {
        Watchlist watchlist = WatchlistAccess.findOrThrow(watchlistRepository, watchlistId);
        if (watchlist.isOwnedBy(userId)) {
            throw new IllegalArgumentException("You already own this watchlist");
        }
        if (watchlist.visibility() != WatchlistVisibility.PUBLIC) {
            throw new ForbiddenException("Only public watchlists can be subscribed without a share code");
        }
        subscriptionRepository.subscribe(userId, watchlist.id());
    }
}
