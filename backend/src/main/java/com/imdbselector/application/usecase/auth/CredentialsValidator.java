package com.imdbselector.application.usecase.auth;

import java.nio.charset.StandardCharsets;

final class CredentialsValidator {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_BCRYPT_BYTES = 72;

    private CredentialsValidator() {
    }

    static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 2 || trimmed.length() > 64) {
            throw new IllegalArgumentException("Username must be between 2 and 64 characters");
        }
        return trimmed;
    }

    static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalArgumentException("Password must not exceed 72 bytes");
        }
    }
}
