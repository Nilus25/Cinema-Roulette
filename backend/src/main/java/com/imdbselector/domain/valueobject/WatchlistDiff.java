package com.imdbselector.domain.valueobject;

import com.imdbselector.domain.model.MediaItem;

import java.util.List;

/**
 * Immutable diff between a Watchlist's current items and an incoming CSV import.
 */
public record WatchlistDiff(List<MediaItem> added, List<MediaItem> removed) {
    public WatchlistDiff {
        added = added == null ? List.of() : List.copyOf(added);
        removed = removed == null ? List.of() : List.copyOf(removed);
    }
}
