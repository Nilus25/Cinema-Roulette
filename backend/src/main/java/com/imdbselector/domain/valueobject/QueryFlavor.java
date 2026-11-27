package com.imdbselector.domain.valueobject;

import java.util.Locale;

public enum QueryFlavor {
    NETFLIX_AND_CHILL,
    MOVIE_GEEK,
    CINEPHILE,
    VO_ENTHUSIAST;

    public static QueryFlavor fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return NETFLIX_AND_CHILL;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return QueryFlavor.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported queryFlavor: " + raw);
        }
    }
}
