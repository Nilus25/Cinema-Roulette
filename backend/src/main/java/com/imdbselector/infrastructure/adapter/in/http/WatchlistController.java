package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.WatchlistService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class WatchlistController {
    private final WatchlistService watchlistService;
    private final AuthController authController;

    public WatchlistController(WatchlistService watchlistService, AuthController authController) {
        this.watchlistService = watchlistService;
        this.authController = authController;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/metadata/filters", ctx -> ctx.json(Map.of(
                "genres", Arrays.stream(Genre.values()).map(Genre::label).toList(),
                "mediaTypes", Arrays.stream(MediaType.values()).map(MediaType::label).toList(),
                "minYear", ReleaseYear.MIN_YEAR
        )));

        app.get("/api/watchlists", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            var list = watchlistService.listAccessibleWatchlists(userId).stream()
                    .map(ApiDtos.WatchlistResponse::from)
                    .toList();
            ctx.json(list);
        });

        app.get("/api/watchlists/public", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            var list = watchlistService.listPublicWatchlists(userId).stream()
                    .map(ApiDtos.WatchlistResponse::from)
                    .toList();
            ctx.json(list);
        });

        app.post("/api/watchlists", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            String name = ctx.formParam("name");
            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                throw new IllegalArgumentException("Missing CSV file part 'file'");
            }
            try (var is = file.content()) {
                var created = watchlistService.importFromCsv(userId, name, is);
                ctx.status(201).json(ApiDtos.WatchlistResponse.from(
                        new WatchlistService.WatchlistSummary(created, true, "You")));
            }
        });

        app.put("/api/watchlists/{watchlistId}/import", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                throw new IllegalArgumentException("Missing CSV file part 'file'");
            }
            try (var is = file.content()) {
                var updated = watchlistService.updateFromCsv(userId, watchlistId, is);
                ctx.json(ApiDtos.WatchlistResponse.from(
                        new WatchlistService.WatchlistSummary(updated, true, "You")));
            }
        });

        app.patch("/api/watchlists/{watchlistId}/visibility", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            var req = ctx.bodyAsClass(ApiDtos.VisibilityRequest.class);
            WatchlistVisibility visibility = WatchlistVisibility.valueOf(req.visibility().trim().toUpperCase());
            var updated = watchlistService.changeVisibility(userId, watchlistId, visibility);
            ctx.json(ApiDtos.WatchlistResponse.from(
                    new WatchlistService.WatchlistSummary(updated, true, "You")));
        });

        app.post("/api/watchlists/subscribe", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            var req = ctx.bodyAsClass(ApiDtos.SubscribeCodeRequest.class);
            watchlistService.subscribeByShareCode(userId, req.shareCode());
            ctx.status(204);
        });

        app.post("/api/watchlists/{watchlistId}/subscribe", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            watchlistService.subscribePublic(userId, watchlistId);
            ctx.status(204);
        });

        app.delete("/api/watchlists/{watchlistId}/subscribe", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            watchlistService.unsubscribe(userId, watchlistId);
            ctx.status(204);
        });

        app.get("/api/watchlists/{watchlistId}/items", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));

            FilterCriteria criteria = parseQueryCriteria(
                    ctx.queryParam("minRating"),
                    ctx.queryParam("maxRating"),
                    ctx.queryParam("minYear"),
                    ctx.queryParam("maxYear"),
                    ctx.queryParam("genres"),
                    ctx.queryParam("mediaTypes"),
                    ctx.queryParam("keywords"),
                    Boolean.parseBoolean(ctx.queryParam("excludeSeen"))
            );

            var items = watchlistService.getWatchlistItems(userId, watchlistId, criteria)
                    .stream()
                    .map(ApiDtos.WatchlistItemResponse::from)
                    .toList();
            ctx.json(items);
        });

        app.delete("/api/watchlists/{watchlistId}", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            watchlistService.deleteWatchlist(userId, watchlistId);
            ctx.status(204);
        });
    }

    private static FilterCriteria parseQueryCriteria(
            String minRating,
            String maxRating,
            String minYear,
            String maxYear,
            String genres,
            String mediaTypes,
            String keywords,
            boolean excludeSeen
    ) {
        Rating minR = (minRating == null || minRating.isBlank()) ? null : Rating.of(minRating);
        Rating maxR = (maxRating == null || maxRating.isBlank()) ? null : Rating.of(maxRating);
        ReleaseYear minY = (minYear == null || minYear.isBlank()) ? null : new ReleaseYear(Integer.parseInt(minYear.trim()));
        ReleaseYear maxY = (maxYear == null || maxYear.isBlank()) ? null : new ReleaseYear(Integer.parseInt(maxYear.trim()));

        Set<Genre> genreSet = splitCsv(genres).stream().map(Genre::fromLabel).collect(Collectors.toSet());
        Set<MediaType> typeSet = splitCsv(mediaTypes).stream().map(MediaType::fromLabel).collect(Collectors.toSet());
        List<String> keywordList = splitCsv(keywords);

        return new FilterCriteria(minR, maxR, minY, maxY, genreSet, typeSet, keywordList, excludeSeen);
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
