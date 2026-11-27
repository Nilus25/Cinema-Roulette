package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.selection.SelectRandomMediaUseCase;
import com.imdbselector.application.usecase.selection.SelectionResult;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import io.javalin.Javalin;

public final class SelectionController {
    private final SelectRandomMediaUseCase selectRandomMedia;
    private final AuthController authController;

    public SelectionController(SelectRandomMediaUseCase selectRandomMedia, AuthController authController) {
        this.selectRandomMedia = selectRandomMedia;
        this.authController = authController;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/watchlists/{watchlistId}/select", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            ApiDtos.SelectRequest req = ctx.body().isBlank()
                    ? ApiDtos.SelectRequest.empty()
                    : ctx.bodyAsClass(ApiDtos.SelectRequest.class);

            SelectionResult result = selectRandomMedia.executeWithReel(watchlistId, userId, req.toCriteria());
            ctx.json(ApiDtos.SelectionResponse.from(result));
        });
    }
}
