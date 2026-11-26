package com.imdbselector.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Canonical IMDB title identifier (e.g. "tt0111161").
 */
public record ImdbId(String value) {
    private static final Pattern IMDB_ID_PATTERN = Pattern.compile("^tt\\d{5,}$");

    public ImdbId {
        Objects.requireNonNull(value, "IMDB ID must not be null");
        value = value.trim();
        if (!IMDB_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid IMDB ID: " + value);
        }
    }
}
