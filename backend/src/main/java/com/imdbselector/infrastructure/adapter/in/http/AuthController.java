package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.auth.AuthSession;
import com.imdbselector.application.usecase.auth.AuthenticateTokenUseCase;
import com.imdbselector.application.usecase.auth.ChangePasswordUseCase;
import com.imdbselector.application.usecase.auth.LoginUserUseCase;
import com.imdbselector.application.usecase.auth.RegisterUserUseCase;
import com.imdbselector.application.usecase.user.GetUserDetailsUseCase;
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

    private final RegisterUserUseCase registerUser;
    private final LoginUserUseCase loginUser;
    private final AuthenticateTokenUseCase authenticateToken;
    private final ChangePasswordUseCase changePassword;
    private final GetUserDetailsUseCase getUserDetails;
    private final boolean cookieSecure;

    public AuthController(
            RegisterUserUseCase registerUser,
            LoginUserUseCase loginUser,
            AuthenticateTokenUseCase authenticateToken,
            ChangePasswordUseCase changePassword,
            GetUserDetailsUseCase getUserDetails
    ) {
        this(registerUser, loginUser, authenticateToken, changePassword, getUserDetails, false);
    }

    public AuthController(
            RegisterUserUseCase registerUser,
            LoginUserUseCase loginUser,
            AuthenticateTokenUseCase authenticateToken,
            ChangePasswordUseCase changePassword,
            GetUserDetailsUseCase getUserDetails,
            boolean cookieSecure
    ) {
        this.registerUser = registerUser;
        this.loginUser = loginUser;
        this.authenticateToken = authenticateToken;
        this.changePassword = changePassword;
        this.getUserDetails = getUserDetails;
        this.cookieSecure = cookieSecure;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/auth/register", ctx -> {
            ApiDtos.AuthRequest req = ctx.bodyAsClass(ApiDtos.AuthRequest.class);
            AuthSession session = registerUser.execute(req.username(), req.password());
            setAuthCookie(ctx, session.token());
            ctx.status(201).json(ApiDtos.UserDetailsResponse.from(getUserDetails.execute(session.user().id())));
        });

        app.post("/api/auth/login", ctx -> {
            ApiDtos.AuthRequest req = ctx.bodyAsClass(ApiDtos.AuthRequest.class);
            AuthSession session = loginUser.execute(req.username(), req.password());
            setAuthCookie(ctx, session.token());
            ctx.json(ApiDtos.UserDetailsResponse.from(getUserDetails.execute(session.user().id())));
        });

        app.post("/api/auth/logout", ctx -> {
            ctx.removeCookie(AUTH_COOKIE, "/");
            ctx.status(204);
        });

        app.get("/api/auth/me", ctx -> {
            UserId userId = requireAuthenticatedUser(ctx);
            ctx.json(ApiDtos.UserDetailsResponse.from(getUserDetails.execute(userId)));
        });

        app.post("/api/auth/password", ctx -> {
            UserId userId = requireAuthenticatedUser(ctx);
            ApiDtos.ChangePasswordRequest req = ctx.bodyAsClass(ApiDtos.ChangePasswordRequest.class);
            AuthSession session = changePassword.execute(userId, req.currentPassword(), req.newPassword());
            setAuthCookie(ctx, session.token());
            ctx.status(204);
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
        UserId userId = authenticateToken.execute(token);
        ctx.attribute(AUTH_USER_ATTR, userId);
        return userId;
    }

    private void setAuthCookie(Context ctx, String token) {
        Cookie cookie = new Cookie(AUTH_COOKIE, token, "/", COOKIE_MAX_AGE_SECONDS, cookieSecure, 0, true);
        cookie.setSameSite(SameSite.LAX);
        ctx.cookie(cookie);
    }
}
