package com.imdbselector.infrastructure.config;

import java.util.Map;

/**
 * Fail-closed security configuration resolved from environment variables.
 */
public record SecuritySettings(
        boolean prod,
        String jwtSecret,
        String corsOrigin,
        boolean cookieSecure,
        long maxUploadBytes
) {
    public static final String DEV_FALLBACK_SECRET = "dev-only-change-me-in-prod-cinema-roulette-secret";
    public static final long DEFAULT_MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MIN_PROD_SECRET_LENGTH = 32;

    public static SecuritySettings fromEnv(Map<String, String> env, String corsOriginOverride) {
        String appEnv = env.getOrDefault("APP_ENV", "dev").trim();
        boolean isProd = "prod".equalsIgnoreCase(appEnv);

        String rawSecret = env.get("JWT_SECRET");
        String resolvedSecret;
        if (isProd) {
            if (rawSecret == null || rawSecret.isBlank()
                    || DEV_FALLBACK_SECRET.equals(rawSecret)
                    || rawSecret.length() < MIN_PROD_SECRET_LENGTH) {
                throw new IllegalStateException(
                        "JWT_SECRET must be set to a strong secret (>= 32 chars) when APP_ENV=prod");
            }
            resolvedSecret = rawSecret;
        } else {
            resolvedSecret = (rawSecret == null || rawSecret.isBlank()) ? DEV_FALLBACK_SECRET : rawSecret;
        }

        String resolvedOrigin = corsOriginOverride != null && !corsOriginOverride.isBlank()
                ? corsOriginOverride.trim()
                : env.getOrDefault("CORS_ORIGIN", "http://localhost:5173").trim();

        if (isProd && ("*".equals(resolvedOrigin) || resolvedOrigin.isBlank())) {
            throw new IllegalStateException("CORS_ORIGIN must be an explicit origin (not '*') when APP_ENV=prod");
        }

        String rawCookieSecure = env.get("COOKIE_SECURE");
        boolean cookieSecure = rawCookieSecure != null && !rawCookieSecure.isBlank()
                ? Boolean.parseBoolean(rawCookieSecure.trim())
                : isProd;

        return new SecuritySettings(isProd, resolvedSecret, resolvedOrigin, cookieSecure, DEFAULT_MAX_UPLOAD_BYTES);
    }
}
