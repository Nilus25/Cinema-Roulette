package com.imdbselector.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecuritySettingsTest {

    @Test
    void prodFailsFastWhenJwtSecretMissingOrWeak() {
        assertThatThrownBy(() -> SecuritySettings.fromEnv(
                Map.of("APP_ENV", "prod", "CORS_ORIGIN", "https://roulette.example.com"),
                null))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> SecuritySettings.fromEnv(
                Map.of("APP_ENV", "prod", "JWT_SECRET", "short", "CORS_ORIGIN", "https://roulette.example.com"),
                null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prodFailsFastWhenCorsOriginIsWildcard() {
        assertThatThrownBy(() -> SecuritySettings.fromEnv(
                Map.of("APP_ENV", "prod", "JWT_SECRET", "0123456789abcdef0123456789abcdef", "CORS_ORIGIN", "*"),
                null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prodEnablesSecureCookieByDefaultWithValidConfig() {
        SecuritySettings settings = SecuritySettings.fromEnv(
                Map.of("APP_ENV", "prod", "JWT_SECRET", "0123456789abcdef0123456789abcdef", "CORS_ORIGIN", "https://roulette.example.com"),
                null);

        assertThat(settings.prod()).isTrue();
        assertThat(settings.cookieSecure()).isTrue();
        assertThat(settings.corsOrigin()).isEqualTo("https://roulette.example.com");
    }

    @Test
    void devAllowsFallbackDefaults() {
        SecuritySettings settings = SecuritySettings.fromEnv(Map.of(), null);

        assertThat(settings.prod()).isFalse();
        assertThat(settings.cookieSecure()).isFalse();
        assertThat(settings.jwtSecret()).isEqualTo(SecuritySettings.DEV_FALLBACK_SECRET);
    }
}
