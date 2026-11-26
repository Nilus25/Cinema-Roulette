package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.Objects;

public final class DeleteWatchlistUseCase {
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final TransactionPort transactionPort;

    public DeleteWatchlistUseCase(
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            TransactionPort transactionPort
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.transactionPort = Objects.requireNonNull(transactionPort);
    }

    public void execute(UserId actorId, WatchlistId watchlistId) {
        transactionPort.inTransaction(() -> {
            WatchlistAccess.findOwnedOrThrow(watchlistRepository, actorId, watchlistId);
            subscriptionRepository.deleteByWatchlistId(watchlistId);
            watchlistRepository.deleteById(watchlistId);
        });
    }
}
