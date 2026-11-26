package com.imdbselector.application.port.out;

import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.List;

public interface WatchlistSubscriptionRepository {
    void subscribe(UserId userId, WatchlistId watchlistId);

    void unsubscribe(UserId userId, WatchlistId watchlistId);

    boolean isSubscribed(UserId userId, WatchlistId watchlistId);

    List<WatchlistId> findSubscribedWatchlistIds(UserId userId);

    void deleteByWatchlistId(WatchlistId watchlistId);

    void deleteByUserId(UserId userId);
}
