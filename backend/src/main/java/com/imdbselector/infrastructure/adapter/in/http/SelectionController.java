package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.MediaSelectionService;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import io.javalin.Javalin;

public final class SelectionController {
    private final MediaSelectionService selectionService;
    private final AuthController authController;

    public SelectionController(MediaSelectionService selectionService, AuthController authController) {
        this.selectionService = selectionService;
        this.authController = authController;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/watchlists/{watchlistId}/select", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            var req = ctx.body().isBlank()
                    ? new ApiDtos.SelectRequest(null, null, null, null, null, null, null, false)
                    : ctx.bodyAsClass(ApiDtos.SelectRequest.class);

            var picked = selectionService.selectRandom(watchlistId, userId, req.toCriteria());
            ctx.json(ApiDtos.MediaItemResponse.from(picked));
        });
    }
}
