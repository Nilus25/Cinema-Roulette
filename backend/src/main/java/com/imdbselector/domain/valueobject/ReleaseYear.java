package com.imdbselector.domain.valueobject;

import java.time.Year;

/**
 * Release year bounded from the dawn of cinema (1888) through near-future announced titles.
 */
public record ReleaseYear(int value) implements Comparable<ReleaseYear> {
    public static final int MIN_YEAR = 1888;

    public ReleaseYear {
        int maxAllowed = Year.now().getValue() + 5;
        if (value < MIN_YEAR || value > maxAllowed) {
            throw new IllegalArgumentException("ReleaseYear must be between " + MIN_YEAR + " and " + maxAllowed + ", got: " + value);
        }
    }

    @Override
    public int compareTo(ReleaseYear other) {
        return Integer.compare(this.value, other.value);
    }
}
