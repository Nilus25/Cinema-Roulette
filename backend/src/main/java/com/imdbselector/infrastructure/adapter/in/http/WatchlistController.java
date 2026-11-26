package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.WatchlistService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/metadata/filters", ctx -> ctx.json(Map.of(
                "genres", Arrays.stream(Genre.values()).map(Genre::label).toList(),
                "mediaTypes", Arrays.stream(MediaType.values()).map(MediaType::label).toList(),
                "minYear", ReleaseYear.MIN_YEAR
        )));

        app.get("/api/watchlists", ctx ->
                ctx.json(watchlistService.listWatchlists().stream().map(ApiDtos.WatchlistResponse::from).toList()));

        app.post("/api/watchlists", ctx -> {
            String name = ctx.formParam("name");
            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                throw new IllegalArgumentException("Missing CSV file part 'file'");
            }
            try (var is = file.content()) {
                var created = watchlistService.importFromCsv(name, is);
                ctx.status(201).json(ApiDtos.WatchlistResponse.from(created));
            }
        });

        app.put("/api/watchlists/{watchlistId}/import", ctx -> {
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                throw new IllegalArgumentException("Missing CSV file part 'file'");
            }
            try (var is = file.content()) {
                var updated = watchlistService.updateFromCsv(watchlistId, is);
                ctx.json(ApiDtos.WatchlistResponse.from(updated));
            }
        });

        app.get("/api/watchlists/{watchlistId}/items", ctx -> {
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            String rawUserId = ctx.queryParam("userId");
            UserId userId = (rawUserId == null || rawUserId.isBlank()) ? null : UserId.fromString(rawUserId);

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

            var items = watchlistService.getWatchlistItems(watchlistId, criteria, userId)
                    .stream()
                    .map(ApiDtos.WatchlistItemResponse::from)
                    .toList();
            ctx.json(items);
        });

        app.delete("/api/watchlists/{watchlistId}", ctx -> {
            WatchlistId watchlistId = WatchlistId.fromString(ctx.pathParam("watchlistId"));
            watchlistService.deleteWatchlist(watchlistId);
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
