package com.imdbselector.application.usecase.watchlist;

import java.math.BigDecimal;
import java.util.List;

public record PagedWatchlistItems(
        List<WatchlistItemView> items,
        int page,
        int size,
        int totalItems,
        int totalPages,
        boolean hasMore,
        int unseenCount,
        BigDecimal avgRating,
        String topGenre
) {
}
