package com.imdbselector.domain.model;

import com.imdbselector.domain.exception.NoMatchingMediaException;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.SelectionMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Domain model representing the Cinema Roulette wheel that spins and picks a MediaItem.
 */
public final class CinemaRoulette {
    private final RandomGenerator random;

    public CinemaRoulette() {
        this(RandomGenerator.getDefault());
    }

    public CinemaRoulette(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public MediaItem spin(List<MediaItem> candidates, SelectionMode mode) {
        List<MediaItem> pool = eligiblePool(candidates, mode);
        return pool.get(random.nextInt(pool.size()));
    }

    public List<MediaItem> sampleReelCandidates(
            List<MediaItem> candidates,
            SelectionMode mode,
            ImdbId excludedId,
            int maxCount
    ) {
        if (maxCount <= 0) {
            return List.of();
        }
        List<MediaItem> pool = eligiblePool(candidates, mode);
        List<MediaItem> mutable = new ArrayList<>();
        for (MediaItem item : pool) {
            if (excludedId == null || !item.imdbId().equals(excludedId)) {
                mutable.add(item);
            }
        }
        if (mutable.isEmpty()) {
            return List.of();
        }
        int limit = Math.min(maxCount, mutable.size());
        for (int i = 0; i < limit; i++) {
            int swapIndex = i + random.nextInt(mutable.size() - i);
            Collections.swap(mutable, i, swapIndex);
        }
        return List.copyOf(mutable.subList(0, limit));
    }

    private static List<MediaItem> eligiblePool(List<MediaItem> candidates, SelectionMode mode) {
        if (candidates == null || candidates.isEmpty()) {
            throw new NoMatchingMediaException("No media items match the selected criteria");
        }

        List<MediaItem> pool = switch (mode) {
            case SelectionMode.Random() -> candidates;
            case SelectionMode.NotSeen(Set<ImdbId> seenIds) -> candidates.stream()
                    .filter(item -> !seenIds.contains(item.imdbId()))
                    .toList();
        };

        if (pool.isEmpty()) {
            throw new NoMatchingMediaException("No unseen media items match the selected criteria");
        }
        return pool;
    }
}
