package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;

public final class ChangeWatchlistVisibilityUseCase {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WatchlistRepository watchlistRepository;
    private final Clock clock;

    public ChangeWatchlistVisibilityUseCase(WatchlistRepository watchlistRepository, Clock clock) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Watchlist execute(UserId actorId, WatchlistId watchlistId, WatchlistVisibility visibility) {
        Watchlist watchlist = WatchlistAccess.findOwnedOrThrow(watchlistRepository, actorId, watchlistId);
        String shareCode = switch (visibility) {
            case PRIVATE -> null;
            case SHARED, PUBLIC -> watchlist.shareCode() != null ? watchlist.shareCode() : generateShareCode();
        };
        watchlist.changeVisibility(visibility, shareCode, clock.instant());
        return watchlistRepository.save(watchlist);
    }

    private static String generateShareCode() {
        byte[] bytes = new byte[5];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
