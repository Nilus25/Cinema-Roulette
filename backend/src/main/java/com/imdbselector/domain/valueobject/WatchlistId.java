package com.imdbselector.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a Watchlist aggregate.
 */
public record WatchlistId(UUID value) {
    public WatchlistId {
        Objects.requireNonNull(value, "WatchlistId must not be null");
    }

    public static WatchlistId random() {
        return new WatchlistId(UUID.randomUUID());
    }

    public static WatchlistId fromString(String raw) {
        return new WatchlistId(UUID.fromString(Objects.requireNonNull(raw, "WatchlistId string must not be null").trim()));
    }
}
