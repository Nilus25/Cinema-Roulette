package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.domain.model.MediaItem;

public record WatchlistItemView(MediaItem item, boolean seen) {
}
