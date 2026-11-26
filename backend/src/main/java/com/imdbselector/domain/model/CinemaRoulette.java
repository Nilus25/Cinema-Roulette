package com.imdbselector.domain.model;

import com.imdbselector.domain.exception.NoMatchingMediaException;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.SelectionMode;

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

        return pool.get(random.nextInt(pool.size()));
    }
}
