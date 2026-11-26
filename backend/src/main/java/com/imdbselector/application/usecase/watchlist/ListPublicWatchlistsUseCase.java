package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ListPublicWatchlistsUseCase {
    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;

    public ListPublicWatchlistsUseCase(WatchlistRepository watchlistRepository, UserRepository userRepository) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public List<WatchlistSummary> execute(UserId userId) {
        List<Watchlist> publicWatchlists = watchlistRepository.findPublicWatchlists();
        Map<UserId, String> ownerNames = WatchlistAccess.ownerNamesMap(userRepository, publicWatchlists);
        return publicWatchlists.stream()
                .map(w -> new WatchlistSummary(w, w.isOwnedBy(userId), ownerNames.getOrDefault(w.ownerUserId(), "Unknown")))
                .toList();
    }
}
