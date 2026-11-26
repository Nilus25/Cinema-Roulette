package com.imdbselector.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a User aggregate.
 */
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId must not be null");
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId fromString(String raw) {
        return new UserId(UUID.fromString(Objects.requireNonNull(raw, "UserId string must not be null").trim()));
    }
}
