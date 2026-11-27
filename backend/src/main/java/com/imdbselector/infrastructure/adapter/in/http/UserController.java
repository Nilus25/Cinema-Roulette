package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.user.DeleteUserUseCase;
import com.imdbselector.application.usecase.user.ExportUserDataUseCase;
import com.imdbselector.application.usecase.user.GetUserDetailsUseCase;
import com.imdbselector.application.usecase.user.MarkMediaSeenUseCase;
import com.imdbselector.application.usecase.user.UnmarkMediaSeenUseCase;
import com.imdbselector.application.usecase.user.UpdateQueryFlavorUseCase;
import com.imdbselector.application.usecase.user.UserExportData;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.QueryFlavor;
import com.imdbselector.domain.valueobject.UserId;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.time.ZoneOffset;

public final class UserController {
    private final GetUserDetailsUseCase getUserDetails;
    private final ExportUserDataUseCase exportUserData;
    private final UpdateQueryFlavorUseCase updateQueryFlavor;
    private final DeleteUserUseCase deleteUser;
    private final MarkMediaSeenUseCase markMediaSeen;
    private final UnmarkMediaSeenUseCase unmarkMediaSeen;
    private final AuthController authController;

    public UserController(
            GetUserDetailsUseCase getUserDetails,
            ExportUserDataUseCase exportUserData,
            UpdateQueryFlavorUseCase updateQueryFlavor,
            DeleteUserUseCase deleteUser,
            MarkMediaSeenUseCase markMediaSeen,
            UnmarkMediaSeenUseCase unmarkMediaSeen,
            AuthController authController
    ) {
        this.getUserDetails = getUserDetails;
        this.exportUserData = exportUserData;
        this.updateQueryFlavor = updateQueryFlavor;
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

        app.patch("/api/users/me/preferences", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ApiDtos.UpdatePreferencesRequest req = ctx.bodyAsClass(ApiDtos.UpdatePreferencesRequest.class);
            if (req.queryFlavor() == null || req.queryFlavor().isBlank()) {
                throw new IllegalArgumentException("queryFlavor must not be blank");
            }
            QueryFlavor flavor = QueryFlavor.fromString(req.queryFlavor());
            User updated = updateQueryFlavor.execute(userId, flavor);
            ctx.json(ApiDtos.UserResponse.from(updated));
        });

        app.get("/api/users/me/export", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            UserExportData exportData = exportUserData.execute(userId);
            String date = LocalDate.now(ZoneOffset.UTC).toString();
            ctx.header("Content-Disposition", "attachment; filename=\"cinema-roulette-backup-" + date + ".json\"");
            ctx.header("Cache-Control", "no-store");
            ctx.json(ApiDtos.UserExportResponse.from(exportData));
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
