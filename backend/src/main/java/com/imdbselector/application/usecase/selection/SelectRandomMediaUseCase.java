package com.imdbselector.application.usecase.selection;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.CinemaRoulette;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.SelectionMode;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class SelectRandomMediaUseCase {
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;
    private final TransactionPort transactionPort;
    private final CinemaRoulette roulette;
    private final Clock clock;

    public SelectRandomMediaUseCase(
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            MediaItemRepository mediaItemRepository,
            UserRepository userRepository,
            TransactionPort transactionPort,
            CinemaRoulette roulette,
            Clock clock
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.roulette = Objects.requireNonNull(roulette);
        this.clock = Objects.requireNonNull(clock);
    }

    public MediaItem execute(WatchlistId watchlistId, UserId actorId, FilterCriteria criteria) {
        return transactionPort.inTransaction(() -> {
            Watchlist watchlist = watchlistRepository.findById(watchlistId)
                    .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found: " + watchlistId.value()));

            boolean subscribed = subscriptionRepository.isSubscribed(actorId, watchlistId);
            if (!watchlist.canBeViewedBy(actorId, subscribed)) {
                throw new ForbiddenException("You do not have access to this watchlist");
            }

            User user = userRepository.findById(actorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorId.value()));

            List<MediaItem> allItems = mediaItemRepository.findByIds(watchlist.mediaItemIds());
            List<MediaItem> filtered = Watchlist.filterItems(allItems, criteria, user.seenMediaIds());

            SelectionMode mode = (criteria != null && criteria.excludeSeen())
                    ? new SelectionMode.NotSeen(user.seenMediaIds())
                    : new SelectionMode.Random();

            MediaItem picked = roulette.spin(filtered, mode);
            user.recordGenerated(picked.imdbId(), clock.instant());
            userRepository.save(user);
            return picked;
        });
    }
}
