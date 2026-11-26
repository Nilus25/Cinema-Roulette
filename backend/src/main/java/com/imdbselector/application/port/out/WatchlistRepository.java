package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WatchlistRepository {
    List<Watchlist> findByOwner(UserId ownerUserId);

    List<Watchlist> findByIds(Collection<WatchlistId> ids);

    List<Watchlist> findPublicWatchlists();

    Optional<Watchlist> findById(WatchlistId id);

    Optional<Watchlist> findByOwnerAndName(UserId ownerUserId, String name);

    Optional<Watchlist> findByShareCode(String shareCode);

    Watchlist save(Watchlist watchlist);

    void deleteById(WatchlistId id);
}
