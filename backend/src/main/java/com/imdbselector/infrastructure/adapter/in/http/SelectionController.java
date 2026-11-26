package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.MediaSelectionService;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import io.javalin.Javalin;

public final class SelectionController {
    private final MediaSelectionService selectionService;

    public SelectionController(MediaSelectionService selectionService) {
        this.selectionService = selectionService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/watchlists/{watchlistId}/select", ctx -> {
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            var req = ctx.body().isBlank()
                    ? new ApiDtos.SelectRequest(null, null, null, null, null, null, null, null, false)
                    : ctx.bodyAsClass(ApiDtos.SelectRequest.class);

            UserId userId = req.userId() == null ? null : new UserId(req.userId());
            var picked = selectionService.selectRandom(watchlistId, userId, req.toCriteria());
            ctx.json(ApiDtos.MediaItemResponse.from(picked));
        });
    }
}
