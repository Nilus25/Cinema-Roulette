package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.UserService;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import io.javalin.Javalin;

public final class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/users", ctx ->
                ctx.json(userService.listUsers().stream().map(ApiDtos.UserResponse::from).toList()));

        app.post("/api/users", ctx -> {
            var req = ctx.bodyAsClass(ApiDtos.CreateUserRequest.class);
            var created = userService.createUser(req.name());
            ctx.status(201).json(ApiDtos.UserResponse.from(created));
        });

        app.get("/api/users/{userId}", ctx -> {
            UserId userId = UserId.fromString(ctx.pathParam("userId"));
            ctx.json(ApiDtos.UserDetailsResponse.from(userService.getUserDetails(userId)));
        });

        app.delete("/api/users/{userId}", ctx -> {
            UserId userId = UserId.fromString(ctx.pathParam("userId"));
            userService.deleteUser(userId);
            ctx.status(204);
        });

        app.post("/api/users/{userId}/seen/{imdbId}", ctx -> {
            UserId userId = UserId.fromString(ctx.pathParam("userId"));
            ImdbId imdbId = new ImdbId(ctx.pathParam("imdbId"));
            var updated = userService.markAsSeen(userId, imdbId);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });

        app.delete("/api/users/{userId}/seen/{imdbId}", ctx -> {
            UserId userId = UserId.fromString(ctx.pathParam("userId"));
            ImdbId imdbId = new ImdbId(ctx.pathParam("imdbId"));
            var updated = userService.unmarkAsSeen(userId, imdbId);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });
    }
}
