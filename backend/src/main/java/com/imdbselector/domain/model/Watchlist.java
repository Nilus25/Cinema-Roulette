package com.imdbselector.domain.model;

import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a named collection of MediaItem references.
 */
public final class Watchlist {
    private final WatchlistId id;
    private final String name;
    private List<ImdbId> mediaItemIds;
    private final Instant createdAt;
    private Instant updatedAt;

    public Watchlist(
            WatchlistId id,
            String name,
            List<ImdbId> mediaItemIds,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Watchlist name must not be blank");
        }
        this.name = name.trim();
        this.mediaItemIds = deduplicate(mediaItemIds);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static Watchlist create(String name, List<ImdbId> mediaItemIds) {
        Instant now = Instant.now();
        return new Watchlist(WatchlistId.random(), name, mediaItemIds, now, now);
    }

    public void replaceItems(List<ImdbId> newMediaItemIds, Instant updatedAt) {
        this.mediaItemIds = deduplicate(newMediaItemIds);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static List<ImdbId> deduplicate(List<ImdbId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    public WatchlistId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<ImdbId> mediaItemIds() {
        return Collections.unmodifiableList(mediaItemIds);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
