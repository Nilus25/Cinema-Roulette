package com.imdbselector.domain.query;

import java.util.Locale;
import java.util.Optional;

public enum NumericField {
    RATING("rating"),
    YEAR("year"),
    RUNTIME("runtime"),
    VOTES("votes");

    private final String canonicalName;

    NumericField(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static Optional<NumericField> fromToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "rating", "imdb_rating", "imdbrating" -> Optional.of(RATING);
            case "year", "release_year", "releaseyear" -> Optional.of(YEAR);
            case "runtime", "runtime_mins", "runtimemins", "duration", "mins" -> Optional.of(RUNTIME);
            case "votes", "num_votes", "numvotes" -> Optional.of(VOTES);
            default -> Optional.empty();
        };
    }
}
