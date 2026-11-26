package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository {
    List<Watchlist> findAll();

    Optional<Watchlist> findById(WatchlistId id);

    Optional<Watchlist> findByName(String name);

    Watchlist save(Watchlist watchlist);

    void deleteById(WatchlistId id);
}
