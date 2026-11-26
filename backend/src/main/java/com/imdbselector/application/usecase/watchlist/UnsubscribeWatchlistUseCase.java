package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.Objects;

public final class UnsubscribeWatchlistUseCase {
    private final WatchlistSubscriptionRepository subscriptionRepository;

    public UnsubscribeWatchlistUseCase(WatchlistSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
    }

    public void execute(UserId userId, WatchlistId watchlistId) {
        subscriptionRepository.unsubscribe(userId, watchlistId);
    }
}
