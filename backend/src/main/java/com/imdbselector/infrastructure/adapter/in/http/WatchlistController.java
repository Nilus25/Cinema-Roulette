package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.watchlist.ChangeWatchlistVisibilityUseCase;
import com.imdbselector.application.usecase.watchlist.DeleteWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.GetWatchlistItemsUseCase;
import com.imdbselector.application.usecase.watchlist.ImportWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.ListAccessibleWatchlistsUseCase;
import com.imdbselector.application.usecase.watchlist.ListPublicWatchlistsUseCase;
import com.imdbselector.application.usecase.watchlist.PagedWatchlistItems;
import com.imdbselector.application.usecase.watchlist.PreviewWatchlistUpdateUseCase;
import com.imdbselector.application.usecase.watchlist.SubscribeWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.UnsubscribeWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.UpdateWatchlistUseCase;
import com.imdbselector.application.usecase.watchlist.WatchlistSummary;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.SortBy;
import com.imdbselector.domain.valueobject.SortDirection;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistDiff;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;
import com.imdbselector.infrastructure.config.SecuritySettings;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class WatchlistController {
    private final ImportWatchlistUseCase importWatchlist;
    private final PreviewWatchlistUpdateUseCase previewWatchlistUpdate;
    private final UpdateWatchlistUseCase updateWatchlist;
    private final DeleteWatchlistUseCase deleteWatchlist;
    private final ChangeWatchlistVisibilityUseCase changeVisibility;
    private final ListAccessibleWatchlistsUseCase listAccessible;
    private final ListPublicWatchlistsUseCase listPublic;
    private final GetWatchlistItemsUseCase getWatchlistItems;
    private final SubscribeWatchlistUseCase subscribeWatchlist;
    private final UnsubscribeWatchlistUseCase unsubscribeWatchlist;
    private final AuthController authController;

    public WatchlistController(
            ImportWatchlistUseCase importWatchlist,
            PreviewWatchlistUpdateUseCase previewWatchlistUpdate,
            UpdateWatchlistUseCase updateWatchlist,
            DeleteWatchlistUseCase deleteWatchlist,
            ChangeWatchlistVisibilityUseCase changeVisibility,
            ListAccessibleWatchlistsUseCase listAccessible,
            ListPublicWatchlistsUseCase listPublic,
            GetWatchlistItemsUseCase getWatchlistItems,
            SubscribeWatchlistUseCase subscribeWatchlist,
            UnsubscribeWatchlistUseCase unsubscribeWatchlist,
            AuthController authController
    ) {
        this.importWatchlist = importWatchlist;
        this.previewWatchlistUpdate = previewWatchlistUpdate;
        this.updateWatchlist = updateWatchlist;
        this.deleteWatchlist = deleteWatchlist;
        this.changeVisibility = changeVisibility;
        this.listAccessible = listAccessible;
        this.listPublic = listPublic;
        this.getWatchlistItems = getWatchlistItems;
        this.subscribeWatchlist = subscribeWatchlist;
        this.unsubscribeWatchlist = unsubscribeWatchlist;
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
            List<ApiDtos.WatchlistResponse> list = listAccessible.execute(userId).stream()
                    .map(ApiDtos.WatchlistResponse::from)
                    .toList();
            ctx.json(list);
        });

        app.get("/api/watchlists/public", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            List<ApiDtos.WatchlistResponse> list = listPublic.execute(userId).stream()
                    .map(ApiDtos.WatchlistResponse::from)
                    .toList();
            ctx.json(list);
        });

        app.post("/api/watchlists", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            String name = ctx.formParam("name");
            UploadedFile file = requireBoundedCsv(ctx);
            try (InputStream is = file.content()) {
                Watchlist created = importWatchlist.execute(userId, name, is);
                ctx.status(201).json(ApiDtos.WatchlistResponse.from(
                        new WatchlistSummary(created, true, "You")));
            }
        });

        app.post("/api/watchlists/{watchlistId}/import/preview", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            UploadedFile file = requireBoundedCsv(ctx);
            try (InputStream is = file.content()) {
                WatchlistDiff diff = previewWatchlistUpdate.execute(userId, watchlistId, is);
                ctx.json(ApiDtos.WatchlistUpdatePreviewResponse.from(diff));
            }
        });

        app.put("/api/watchlists/{watchlistId}/import", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            UploadedFile file = requireBoundedCsv(ctx);
            Set<ImdbId> excludeAddedIds = parseImdbIdSet(ctx.formParam("excludeAddedIds"));
            Set<ImdbId> keepRemovedIds = parseImdbIdSet(ctx.formParam("keepRemovedIds"));
            Set<ImdbId> markSeenIds = parseImdbIdSet(ctx.formParam("markSeenIds"));
            try (InputStream is = file.content()) {
                Watchlist updated = updateWatchlist.execute(
                        userId, watchlistId, is, excludeAddedIds, keepRemovedIds, markSeenIds);
                ctx.json(ApiDtos.WatchlistResponse.from(
                        new WatchlistSummary(updated, true, "You")));
            }
        });

        app.patch("/api/watchlists/{watchlistId}/visibility", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            ApiDtos.VisibilityRequest req = ctx.bodyAsClass(ApiDtos.VisibilityRequest.class);
            WatchlistVisibility visibility = WatchlistVisibility.valueOf(req.visibility().trim().toUpperCase());
            Watchlist updated = changeVisibility.execute(userId, watchlistId, visibility);
            ctx.json(ApiDtos.WatchlistResponse.from(
                    new WatchlistSummary(updated, true, "You")));
        });

        app.post("/api/watchlists/subscribe", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            ApiDtos.SubscribeCodeRequest req = ctx.bodyAsClass(ApiDtos.SubscribeCodeRequest.class);
            subscribeWatchlist.byShareCode(userId, req.shareCode());
            ctx.status(204);
        });

        app.post("/api/watchlists/{watchlistId}/subscribe", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            subscribeWatchlist.toPublic(userId, watchlistId);
            ctx.status(204);
        });

        app.delete("/api/watchlists/{watchlistId}/subscribe", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            unsubscribeWatchlist.execute(userId, watchlistId);
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
                    ctx.queryParam("minRuntime"),
                    ctx.queryParam("maxRuntime"),
                    ctx.queryParam("minVotes"),
                    ctx.queryParam("maxVotes"),
                    ctx.queryParam("genres"),
                    ctx.queryParam("mediaTypes"),
                    ctx.queryParam("keywords"),
                    Boolean.parseBoolean(ctx.queryParam("excludeSeen")),
                    ctx.queryParam("query"),
                    ctx.queryParam("quickSearch")
            );
            SortBy sortBy = SortBy.fromString(ctx.queryParam("sortBy"));
            SortDirection sortDir = SortDirection.fromString(ctx.queryParam("sortDir"), null);
            Integer pageParam = parseOptionalInt("page", ctx.queryParam("page"));
            Integer sizeParam = parseOptionalInt("size", ctx.queryParam("size"));
            int page = pageParam == null ? 0 : pageParam;
            int size = sizeParam == null ? GetWatchlistItemsUseCase.DEFAULT_PAGE_SIZE : sizeParam;

            PagedWatchlistItems result = getWatchlistItems.executePaged(
                    userId, watchlistId, criteria, sortBy, sortDir, page, size);
            ctx.json(ApiDtos.PagedWatchlistItemsResponse.from(result));
        });

        app.delete("/api/watchlists/{watchlistId}", ctx -> {
            UserId userId = authController.requireAuthenticatedUser(ctx);
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            deleteWatchlist.execute(userId, watchlistId);
            ctx.status(204);
        });
    }

    private static FilterCriteria parseQueryCriteria(
            String minRating,
            String maxRating,
            String minYear,
            String maxYear,
            String minRuntime,
            String maxRuntime,
            String minVotes,
            String maxVotes,
            String genres,
            String mediaTypes,
            String keywords,
            boolean excludeSeen,
            String query,
            String quickSearch
    ) {
        Rating minR = (minRating == null || minRating.isBlank()) ? null : Rating.of(minRating);
        Rating maxR = (maxRating == null || maxRating.isBlank()) ? null : Rating.of(maxRating);
        ReleaseYear minY = (minYear == null || minYear.isBlank()) ? null : new ReleaseYear(parseIntParam("minYear", minYear));
        ReleaseYear maxY = (maxYear == null || maxYear.isBlank()) ? null : new ReleaseYear(parseIntParam("maxYear", maxYear));
        Integer minRt = parseOptionalInt("minRuntime", minRuntime);
        Integer maxRt = parseOptionalInt("maxRuntime", maxRuntime);
        Integer minV = parseOptionalInt("minVotes", minVotes);
        Integer maxV = parseOptionalInt("maxVotes", maxVotes);

        Set<Genre> genreSet = splitCsv(genres).stream().map(Genre::fromLabel).collect(Collectors.toSet());
        Set<MediaType> typeSet = splitCsv(mediaTypes).stream().map(MediaType::fromLabel).collect(Collectors.toSet());
        List<String> keywordList = splitCsv(keywords);

        return new FilterCriteria(minR, maxR, minY, maxY, minRt, maxRt, minV, maxV, genreSet, typeSet, keywordList, excludeSeen, query, quickSearch);
    }

    private static Integer parseOptionalInt(String paramName, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseIntParam(paramName, raw);
    }

    private static int parseIntParam(String paramName, String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for '" + paramName + "': " + raw);
        }
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

    private static Set<ImdbId> parseImdbIdSet(String raw) {
        return splitCsv(raw).stream()
                .map(ImdbId::new)
                .collect(Collectors.toSet());
    }

    private static UploadedFile requireBoundedCsv(Context ctx) {
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            throw new IllegalArgumentException("Missing CSV file part 'file'");
        }
        if (file.size() > SecuritySettings.DEFAULT_MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("CSV file exceeds maximum allowed size of 5 MB");
        }
        return file;
    }
}
