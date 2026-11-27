package com.imdbselector.domain.model;

import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistDiff;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root representing a named collection of MediaItem references owned by a User.
 */
public final class Watchlist {
    private final WatchlistId id;
    private final UserId ownerUserId;
    private final String name;
    private WatchlistVisibility visibility;
    private String shareCode;
    private List<ImdbId> mediaItemIds;
    private final Instant createdAt;
    private Instant updatedAt;

    public Watchlist(
            WatchlistId id,
            UserId ownerUserId,
            String name,
            WatchlistVisibility visibility,
            String shareCode,
            List<ImdbId> mediaItemIds,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerUserId = ownerUserId;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Watchlist name must not be blank");
        }
        this.name = name.trim();
        this.visibility = visibility == null ? WatchlistVisibility.PRIVATE : visibility;
        this.shareCode = shareCode;
        this.mediaItemIds = deduplicate(mediaItemIds);
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static Watchlist create(UserId ownerUserId, String name, List<ImdbId> mediaItemIds) {
        Instant now = Instant.now();
        return new Watchlist(
                WatchlistId.random(),
                Objects.requireNonNull(ownerUserId, "ownerUserId must not be null"),
                name,
                WatchlistVisibility.PRIVATE,
                null,
                mediaItemIds,
                now,
                now
        );
    }

    public boolean isOwnedBy(UserId userId) {
        return userId != null && ownerUserId != null && ownerUserId.equals(userId);
    }

    public boolean canBeViewedBy(UserId userId, boolean isSubscribed) {
        if (isOwnedBy(userId)) {
            return true;
        }
        if (visibility == WatchlistVisibility.PUBLIC) {
            return true;
        }
        return visibility == WatchlistVisibility.SHARED && isSubscribed;
    }

    public void replaceItems(List<ImdbId> newMediaItemIds, Instant updatedAt) {
        this.mediaItemIds = deduplicate(newMediaItemIds);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void changeVisibility(WatchlistVisibility newVisibility, String newShareCode, Instant updatedAt) {
        this.visibility = Objects.requireNonNull(newVisibility, "visibility must not be null");
        this.shareCode = newShareCode;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public WatchlistDiff diffAgainst(List<MediaItem> currentItems, List<MediaItem> incomingItems) {
        Set<ImdbId> currentIds = new LinkedHashSet<>(this.mediaItemIds);
        Map<ImdbId, MediaItem> incomingById = new LinkedHashMap<>();
        if (incomingItems != null) {
            for (MediaItem item : incomingItems) {
                incomingById.putIfAbsent(item.imdbId(), item);
            }
        }

        List<MediaItem> added = incomingById.values().stream()
                .filter(item -> !currentIds.contains(item.imdbId()))
                .toList();

        List<MediaItem> removed = (currentItems == null ? List.<MediaItem>of() : currentItems).stream()
                .filter(item -> !incomingById.containsKey(item.imdbId()))
                .toList();

        return new WatchlistDiff(added, removed);
    }

    /**
     * Filters the given catalog items belonging to this watchlist according to FilterCriteria.
     */
    public static List<MediaItem> filterItems(List<MediaItem> items, FilterCriteria criteria) {
        return filterItems(items, criteria, Set.of());
    }

    public static List<MediaItem> filterItems(List<MediaItem> items, FilterCriteria criteria, Set<ImdbId> seenIds) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        FilterCriteria effective = criteria == null ? FilterCriteria.unfiltered() : criteria;
        Set<ImdbId> effectiveSeen = seenIds == null ? Set.of() : seenIds;
        return items.stream()
                .filter(item -> item.matches(effective, effectiveSeen.contains(item.imdbId())))
                .toList();
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

    public UserId ownerUserId() {
        return ownerUserId;
    }

    public String name() {
        return name;
    }

    public WatchlistVisibility visibility() {
        return visibility;
    }

    public String shareCode() {
        return shareCode;
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
