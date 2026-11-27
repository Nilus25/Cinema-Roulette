package com.imdbselector.domain.valueobject;

import java.util.Locale;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection fromString(String raw, SortDirection fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return SortDirection.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported sortDir value: " + raw);
        }
    }
}
