package com.imdbselector.domain.service;

import com.imdbselector.domain.exception.NoMatchingMediaException;
import com.imdbselector.domain.model.MediaItem;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Pure domain service that selects a random MediaItem from filtered candidates.
 */
public final class RandomMediaSelector {
    private final RandomGenerator random;

    public RandomMediaSelector() {
        this(RandomGenerator.getDefault());
    }

    public RandomMediaSelector(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public MediaItem select(List<MediaItem> candidates, SelectionMode mode) {
        if (candidates == null || candidates.isEmpty()) {
            throw new NoMatchingMediaException("No media items match the selected criteria");
        }

        List<MediaItem> pool = switch (mode) {
            case SelectionMode.Random() -> candidates;
            case SelectionMode.NotSeen(var seenIds) -> candidates.stream()
                    .filter(item -> !seenIds.contains(item.imdbId()))
                    .toList();
        };

        if (pool.isEmpty()) {
            throw new NoMatchingMediaException("No unseen media items match the selected criteria");
        }

        return pool.get(random.nextInt(pool.size()));
    }
}
