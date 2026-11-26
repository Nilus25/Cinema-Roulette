package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.domain.model.Watchlist;

public record WatchlistSummary(Watchlist watchlist, boolean owned, String ownerName) {
}
