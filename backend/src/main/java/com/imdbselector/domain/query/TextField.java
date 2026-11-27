package com.imdbselector.domain.query;

import java.util.Locale;
import java.util.Optional;

public enum TextField {
    TITLE("title"),
    DIRECTOR("director"),
    KEYWORD("keyword");

    private final String canonicalName;

    TextField(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static Optional<TextField> fromToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "title" -> Optional.of(TITLE);
            case "director", "directors" -> Optional.of(DIRECTOR);
            case "keyword", "keywords", "text" -> Optional.of(KEYWORD);
            default -> Optional.empty();
        };
    }
}
