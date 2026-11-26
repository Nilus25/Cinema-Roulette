package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.user.DeleteUserUseCase;
import com.imdbselector.application.usecase.user.GetUserDetailsUseCase;
import com.imdbselector.application.usecase.user.MarkMediaSeenUseCase;
import com.imdbselector.application.usecase.user.UnmarkMediaSeenUseCase;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import io.javalin.Javalin;

public final class UserController {
    private final GetUserDetailsUseCase getUserDetails;
    private final DeleteUserUseCase deleteUser;
    private final MarkMediaSeenUseCase markMediaSeen;
    private final UnmarkMediaSeenUseCase unmarkMediaSeen;
    private final AuthController authController;

    public UserController(
            GetUserDetailsUseCase getUserDetails,
            DeleteUserUseCase deleteUser,
            MarkMediaSeenUseCase markMediaSeen,
            UnmarkMediaSeenUseCase unmarkMediaSeen,
            AuthController authController
    ) {
        this.getUserDetails = getUserDetails;
        this.deleteUser = deleteUser;
        this.markMediaSeen = markMediaSeen;
        this.unmarkMediaSeen = unmarkMediaSeen;
        this.authController = authController;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/users/me", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ctx.json(ApiDtos.UserDetailsResponse.from(getUserDetails.execute(userId)));
        });

        app.delete("/api/users/me", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            deleteUser.execute(userId);
            ctx.removeCookie(AuthController.AUTH_COOKIE, "/");
            ctx.status(204);
        });

        app.post("/api/users/me/seen/{imdbId}", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ImdbId imdbId = new ImdbId(ctx.pathParam("imdbId"));
            User updated = markMediaSeen.execute(userId, imdbId);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });

        app.delete("/api/users/me/seen/{imdbId}", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ImdbId imdbId = new ImdbId(ctx.pathParam("imdbId"));
            User updated = unmarkMediaSeen.execute(userId, imdbId);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });
    }
}
