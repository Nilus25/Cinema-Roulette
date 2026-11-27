package com.imdbselector.domain.valueobject;

import java.util.Locale;

public enum SortBy {
    DEFAULT,
    RATING,
    YEAR,
    RUNTIME,
    VOTES,
    TITLE,
    UNSEEN;

    public static SortBy fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return SortBy.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported sortBy value: " + raw);
        }
    }
}
