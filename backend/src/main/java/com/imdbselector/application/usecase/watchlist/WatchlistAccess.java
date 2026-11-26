package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class WatchlistAccess {
    private WatchlistAccess() {
    }

    static Watchlist findOrThrow(WatchlistRepository repo, WatchlistId watchlistId) {
        return repo.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found: " + watchlistId.value()));
    }

    static Watchlist findOwnedOrThrow(WatchlistRepository repo, UserId actorId, WatchlistId watchlistId) {
        Watchlist watchlist = findOrThrow(repo, watchlistId);
        if (!watchlist.isOwnedBy(actorId)) {
            throw new ForbiddenException("Only the watchlist owner can modify this watchlist");
        }
        return watchlist;
    }

    static Watchlist findViewableOrThrow(
            WatchlistRepository repo,
            WatchlistSubscriptionRepository subRepo,
            UserId actorId,
            WatchlistId watchlistId
    ) {
        Watchlist watchlist = findOrThrow(repo, watchlistId);
        boolean subscribed = subRepo.isSubscribed(actorId, watchlistId);
        if (!watchlist.canBeViewedBy(actorId, subscribed)) {
            throw new ForbiddenException("You do not have access to this watchlist");
        }
        return watchlist;
    }

    static Map<UserId, String> ownerNamesMap(UserRepository userRepository, List<Watchlist> watchlists) {
        Set<UserId> ownerIds = watchlists.stream()
                .map(Watchlist::ownerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userRepository.findNamesByIds(ownerIds);
    }
}
