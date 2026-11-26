package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.UserService;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import io.javalin.Javalin;

public final class UserController {
    private final UserService userService;
    private final AuthController authController;

    public UserController(UserService userService, AuthController authController) {
        this.userService = userService;
        this.authController = authController;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/users/me", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ctx.json(ApiDtos.UserDetailsResponse.from(userService.getUserDetails(userId)));
        });

        app.delete("/api/users/me", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            userService.deleteUser(userId);
            ctx.removeCookie(AuthController.AUTH_COOKIE, "/");
            ctx.status(204);
        });

        app.post("/api/users/me/seen/{imdbId}", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ImdbId imdbId = new ImdbId(ctx.pathParam("imdbId"));
            var updated = userService.markAsSeen(userId, imdbId);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });

        app.delete("/api/users/me/seen/{imdbId}", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ImdbId imdbId = new ImdbId(ctx.pathParam("imdbId"));
            var updated = userService.unmarkAsSeen(userId, imdbId);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });
    }
}
