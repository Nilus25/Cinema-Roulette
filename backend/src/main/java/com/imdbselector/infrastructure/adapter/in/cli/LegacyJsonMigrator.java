package com.imdbselector.infrastructure.adapter.in.cli;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.infrastructure.adapter.out.persistence.DatabaseConfig;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqMediaItemRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqUserRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Idempotent CLI migrator for importing legacy data/user/*_user.json and data/watchlist/*_watchlist.json into PostgreSQL.
 */
public final class LegacyJsonMigrator {
    private static final Logger log = LoggerFactory.getLogger(LegacyJsonMigrator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        Path dataRoot = Path.of(args.length > 0 ? args[0] : "data");
        String jdbcUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/imdbselector");
        String dbUser = System.getenv().getOrDefault("DB_USER", "imdb");
        String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "imdb");

        try (var ds = DatabaseConfig.createDataSource(jdbcUrl, dbUser, dbPass)) {
            DatabaseConfig.migrate(ds);
            var dsl = DatabaseConfig.createDsl(ds);
            var migrator = new LegacyJsonMigrator(
                    new JooqMediaItemRepository(dsl),
                    new JooqWatchlistRepository(dsl),
                    new JooqUserRepository(dsl)
            );
            migrator.migrate(dataRoot);
        }
    }

    private final MediaItemRepository mediaRepo;
    private final WatchlistRepository watchlistRepo;
    private final UserRepository userRepo;

    public LegacyJsonMigrator(MediaItemRepository mediaRepo, WatchlistRepository watchlistRepo, UserRepository userRepo) {
        this.mediaRepo = mediaRepo;
        this.watchlistRepo = watchlistRepo;
        this.userRepo = userRepo;
    }

    public void migrate(Path dataRoot) throws IOException {
        Path watchlistDir = dataRoot.resolve("watchlist");
        if (Files.isDirectory(watchlistDir)) {
            try (Stream<Path> files = Files.list(watchlistDir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith("_watchlist.json")).toList()) {
                    importWatchlistFile(file);
                }
            }
        }
        Path userDir = dataRoot.resolve("user");
        if (Files.isDirectory(userDir)) {
            try (Stream<Path> files = Files.list(userDir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith("_user.json")).toList()) {
                    importUserFile(file);
                }
            }
        }
    }

    private void importWatchlistFile(Path file) throws IOException {
        LegacyWatchlistJson raw = MAPPER.readValue(file.toFile(), LegacyWatchlistJson.class);
        if (raw.name() == null || raw.name().isBlank()) {
            return;
        }
        List<MediaItem> items = toMediaItems(raw.watchList());
        mediaRepo.upsertAll(items);
        List<ImdbId> ids = items.stream().map(MediaItem::imdbId).toList();

        Watchlist watchlist = watchlistRepo.findByName(raw.name().trim())
                .map(existing -> {
                    existing.replaceItems(ids, Instant.now());
                    return existing;
                })
                .orElseGet(() -> Watchlist.create(raw.name().trim(), ids));
        watchlistRepo.save(watchlist);
        log.info("Migrated watchlist '{}' ({} items)", watchlist.name(), ids.size());
    }

    private void importUserFile(Path file) throws IOException {
        LegacyUserJson raw = MAPPER.readValue(file.toFile(), LegacyUserJson.class);
        if (raw.name() == null || raw.name().isBlank()) {
            return;
        }
        List<MediaItem> seenItems = toMediaItems(raw.seenMovies());
        List<MediaItem> allToUpsert = new ArrayList<>(seenItems);
        MediaItem lastGenerated = toMediaItem(raw.lastMovieGenerated());
        if (lastGenerated != null) {
            allToUpsert.add(lastGenerated);
        }
        mediaRepo.upsertAll(allToUpsert);

        User user = userRepo.findByName(raw.name().trim()).orElseGet(() -> User.create(raw.name().trim()));
        for (MediaItem seen : seenItems) {
            user.markAsSeen(seen.imdbId());
        }
        if (lastGenerated != null) {
            user.recordGenerated(lastGenerated.imdbId(), Instant.now());
        }
        userRepo.save(user);
        log.info("Migrated user '{}' ({} seen items)", user.name(), seenItems.size());
    }

    private static List<MediaItem> toMediaItems(List<LegacyMovieJson> rawMovies) {
        if (rawMovies == null) {
            return List.of();
        }
        List<MediaItem> result = new ArrayList<>();
        for (LegacyMovieJson m : rawMovies) {
            MediaItem item = toMediaItem(m);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private static MediaItem toMediaItem(LegacyMovieJson m) {
        if (m == null || m.IMDBTag() == null || m.IMDBTag().isBlank() || m.title() == null || m.title().isBlank()) {
            return null;
        }
        ReleaseYear year = parseYear(m.year());
        Rating rating = parseRating(m.rating());
        Set<Genre> genres = m.genres() == null ? Set.of() : m.genres().stream().map(Genre::fromLabel).collect(Collectors.toSet());
        return new MediaItem(
                new ImdbId(m.IMDBTag()),
                m.title(),
                m.director(),
                year,
                rating,
                MediaType.fromLabel(m.type()),
                m.URL(),
                genres,
                MetadataSource.CSV
        );
    }

    private static ReleaseYear parseYear(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return new ReleaseYear(Integer.parseInt(raw.trim())); } catch (IllegalArgumentException e) { return null; }
    }

    private static Rating parseRating(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Rating.of(raw); } catch (IllegalArgumentException e) { return null; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LegacyUserJson(String name, List<LegacyMovieJson> seenMovies, LegacyMovieJson lastMovieGenerated) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LegacyWatchlistJson(String name, List<LegacyMovieJson> watchList) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LegacyMovieJson(String title, String IMDBTag, String URL, String director, String year, String rating, String type, List<String> genres) {
    }
}
