package com.imdbselector.application.usecase.user;

import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Objects;

public final class DeleteUserUseCase {
    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final TransactionPort transactionPort;

    public DeleteUserUseCase(
            UserRepository userRepository,
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            TransactionPort transactionPort
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.transactionPort = Objects.requireNonNull(transactionPort);
    }

    public void execute(UserId userId) {
        transactionPort.inTransaction(() -> {
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));

            subscriptionRepository.deleteByUserId(userId);
            List<Watchlist> owned = watchlistRepository.findByOwner(userId);
            for (Watchlist watchlist : owned) {
                subscriptionRepository.deleteByWatchlistId(watchlist.id());
                watchlistRepository.deleteById(watchlist.id());
            }
            userRepository.deleteById(userId);
        });
    }
}
