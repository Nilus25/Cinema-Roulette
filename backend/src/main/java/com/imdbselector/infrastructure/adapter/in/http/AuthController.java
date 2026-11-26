package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.AuthService;
import com.imdbselector.application.usecase.UserService;
import com.imdbselector.domain.valueobject.UserId;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;

import java.time.Duration;

public final class AuthController {
    public static final String AUTH_COOKIE = "cr_token";
    public static final String AUTH_USER_ATTR = "authUserId";
    private static final int COOKIE_MAX_AGE_SECONDS = (int) Duration.ofDays(14).toSeconds();

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/auth/register", ctx -> {
            var req = ctx.bodyAsClass(ApiDtos.AuthRequest.class);
            var session = authService.register(req.username(), req.password());
            setAuthCookie(ctx, session.token());
            ctx.status(201).json(ApiDtos.UserDetailsResponse.from(userService.getUserDetails(session.user().id())));
        });

        app.post("/api/auth/login", ctx -> {
            var req = ctx.bodyAsClass(ApiDtos.AuthRequest.class);
            var session = authService.login(req.username(), req.password());
            setAuthCookie(ctx, session.token());
            ctx.json(ApiDtos.UserDetailsResponse.from(userService.getUserDetails(session.user().id())));
        });

        app.post("/api/auth/logout", ctx -> {
            ctx.removeCookie(AUTH_COOKIE, "/");
            ctx.status(204);
        });

        app.get("/api/auth/me", ctx -> {
            UserId userId = requireAuthenticatedUser(ctx);
            ctx.json(ApiDtos.UserDetailsResponse.from(userService.getUserDetails(userId)));
        });
    }

    public UserId requireAuthenticatedUser(Context ctx) {
        UserId cached = ctx.attribute(AUTH_USER_ATTR);
        if (cached != null) {
            return cached;
        }
        String token = ctx.cookie(AUTH_COOKIE);
        if (token == null || token.isBlank()) {
            String header = ctx.header("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring("Bearer ".length()).trim();
            }
        }
        UserId userId = authService.authenticateToken(token);
        ctx.attribute(AUTH_USER_ATTR, userId);
        return userId;
    }

    private static void setAuthCookie(Context ctx, String token) {
        Cookie cookie = new Cookie(AUTH_COOKIE, token, "/", COOKIE_MAX_AGE_SECONDS, false, 0, true);
        cookie.setSameSite(SameSite.LAX);
        ctx.cookie(cookie);
    }
}
