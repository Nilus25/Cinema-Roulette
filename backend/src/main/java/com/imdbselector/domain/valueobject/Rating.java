package com.imdbselector.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * IMDB rating bounded between 0.0 and 10.0.
 */
public record Rating(BigDecimal value) implements Comparable<Rating> {
    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.TEN;

    public Rating {
        Objects.requireNonNull(value, "Rating must not be null");
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
            throw new IllegalArgumentException("Rating must be between 0.0 and 10.0, got: " + value);
        }
    }

    public static Rating of(double value) {
        return new Rating(BigDecimal.valueOf(value));
    }

    public static Rating of(String raw) {
        return new Rating(new BigDecimal(raw.trim()));
    }

    @Override
    public int compareTo(Rating other) {
        return this.value.compareTo(other.value);
    }
}
