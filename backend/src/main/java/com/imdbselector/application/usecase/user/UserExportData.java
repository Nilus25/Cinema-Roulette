package com.imdbselector.application.usecase.user;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;

import java.time.Instant;
import java.util.List;

/**
 * Complete personal data backup snapshot for a single authenticated user.
 */
public record UserExportData(
        Instant exportedAt,
        User user,
        List<MediaItem> seenMovies,
        List<OwnedWatchlistExport> ownedWatchlists,
        List<SubscribedWatchlistSummary> subscriptions
) {
    public record OwnedWatchlistExport(
            Watchlist watchlist,
            List<MediaItem> items
    ) {
    }

    public record SubscribedWatchlistSummary(
            Watchlist watchlist,
            String ownerName
    ) {
    }
}
