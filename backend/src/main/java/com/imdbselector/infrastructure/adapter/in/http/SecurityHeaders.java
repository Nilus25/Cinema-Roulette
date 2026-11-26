package com.imdbselector.infrastructure.adapter.in.http;

import io.javalin.Javalin;

/**
 * Applies defensive OWASP HTTP security headers to API responses.
 */
public final class SecurityHeaders {
    private SecurityHeaders() {
    }

    public static void register(Javalin app) {
        app.after(ctx -> {
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
            ctx.header("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            if (ctx.path().startsWith("/api/auth")) {
                ctx.header("Cache-Control", "no-store");
            }
        });
    }
}
