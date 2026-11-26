package com.imdbselector.infrastructure.adapter.in.cli;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.PasswordHasherPort;
import com.imdbselector.application.port.out.TransactionPort;
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
import com.imdbselector.infrastructure.adapter.out.persistence.JooqTransactionManager;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqUserRepository;
import com.imdbselector.infrastructure.adapter.out.persistence.JooqWatchlistRepository;
import com.imdbselector.infrastructure.adapter.out.security.BcryptPasswordHasher;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        CliOptions options = CliOptions.parse(args);
        String jdbcUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/imdbselector");
        String dbUser = System.getenv().getOrDefault("DB_USER", "imdb");
        String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "imdb");

        try (HikariDataSource ds = DatabaseConfig.createDataSource(jdbcUrl, dbUser, dbPass)) {
            DatabaseConfig.migrate(ds);
            JooqTransactionManager txManager = new JooqTransactionManager(ds);
            DSLContext dsl = DatabaseConfig.createDsl(txManager);
            LegacyJsonMigrator migrator = new LegacyJsonMigrator(
                    new JooqMediaItemRepository(dsl),
                    new JooqWatchlistRepository(dsl),
                    new JooqUserRepository(dsl),
                    new BcryptPasswordHasher(),
                    txManager
            );
            migrator.migrate(options.dataRoot(), options.ownerUsername(), options.defaultPassword());
        }
    }

    private final MediaItemRepository mediaRepo;
    private final WatchlistRepository watchlistRepo;
    private final UserRepository userRepo;
    private final PasswordHasherPort passwordHasher;
    private final TransactionPort transactionPort;

    public LegacyJsonMigrator(
            MediaItemRepository mediaRepo,
            WatchlistRepository watchlistRepo,
            UserRepository userRepo,
            PasswordHasherPort passwordHasher,
            TransactionPort transactionPort
    ) {
        this.mediaRepo = mediaRepo;
        this.watchlistRepo = watchlistRepo;
        this.userRepo = userRepo;
        this.passwordHasher = passwordHasher;
        this.transactionPort = transactionPort;
    }

    public void migrate(Path dataRoot, String ownerUsername, String defaultPassword) throws IOException {
        Path userDir = dataRoot.resolve("user");
        if (Files.isDirectory(userDir)) {
            try (Stream<Path> files = Files.list(userDir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith("_user.json")).toList()) {
                    importUserFile(file, defaultPassword);
                }
            }
        }

        Path watchlistDir = dataRoot.resolve("watchlist");
        if (Files.isDirectory(watchlistDir)) {
            User owner = resolveWatchlistOwner(ownerUsername);
            try (Stream<Path> files = Files.list(watchlistDir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith("_watchlist.json")).toList()) {
                    importWatchlistFile(file, owner);
                }
            }
        }
    }

    private void importUserFile(Path file, String defaultPassword) throws IOException {
        LegacyUserJson raw = readJsonFile(file, LegacyUserJson.class);
        if (raw.name() == null || raw.name().isBlank()) {
            return;
        }
        String username = raw.name().trim();
        List<MediaItem> seenItems = toMediaItems(raw.seenMovies());
        List<MediaItem> allToUpsert = new ArrayList<>(seenItems);
        MediaItem lastGenerated = toMediaItem(raw.lastMovieGenerated());
        if (lastGenerated != null) {
            allToUpsert.add(lastGenerated);
        }

        transactionPort.inTransaction(() -> {
            mediaRepo.upsertAll(allToUpsert);
            Optional<User> existing = userRepo.findByName(username);
            User user = existing.orElseGet(() -> registerWithInitialPassword(username, defaultPassword));
            for (MediaItem seen : seenItems) {
                user.markAsSeen(seen.imdbId());
            }
            if (lastGenerated != null) {
                user.recordGenerated(lastGenerated.imdbId(), Instant.now());
            }
            userRepo.save(user);
            log.info("Migrated user '{}' ({} seen items)", user.name(), seenItems.size());
        });
    }

    private User registerWithInitialPassword(String username, String defaultPassword) {
        if (defaultPassword == null || defaultPassword.length() < 6
                || defaultPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Provide MIGRATE_DEFAULT_PASSWORD or --password (6-72 bytes) for migrated user: " + username);
        }
        return User.register(username, passwordHasher.hash(defaultPassword));
    }

    private User resolveWatchlistOwner(String ownerUsername) {
        if (ownerUsername != null && !ownerUsername.isBlank()) {
            return userRepo.findByName(ownerUsername.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Watchlist owner user not found: " + ownerUsername));
        }
        List<User> allUsers = userRepo.findAll();
        if (allUsers.size() == 1) {
            return allUsers.get(0);
        }
        throw new IllegalArgumentException(
                "Multiple or zero users in database; specify --owner <username> or MIGRATE_OWNER");
    }

    private void importWatchlistFile(Path file, User owner) throws IOException {
        LegacyWatchlistJson raw = readJsonFile(file, LegacyWatchlistJson.class);
        if (raw.name() == null || raw.name().isBlank()) {
            return;
        }
        String watchlistName = raw.name().trim();
        List<MediaItem> items = toMediaItems(raw.watchList());
        List<ImdbId> ids = items.stream().map(MediaItem::imdbId).toList();

        transactionPort.inTransaction(() -> {
            mediaRepo.upsertAll(items);
            Watchlist watchlist = watchlistRepo.findByOwnerAndName(owner.id(), watchlistName)
                    .map(existing -> {
                        existing.replaceItems(ids, Instant.now());
                        return existing;
                    })
                    .orElseGet(() -> Watchlist.create(owner.id(), watchlistName, ids));
            watchlistRepo.save(watchlist);
            log.info("Migrated watchlist '{}' for owner '{}' ({} items)", watchlist.name(), owner.name(), ids.size());
        });
    }

    private static <T> T readJsonFile(Path file, Class<T> type) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String content;
        try {
            content = utf8Decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (MalformedInputException e) {
            content = Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString();
        }
        return MAPPER.readValue(content, type);
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

    private record CliOptions(Path dataRoot, String ownerUsername, String defaultPassword) {
        static CliOptions parse(String[] args) {
            String data = System.getenv().getOrDefault("MIGRATE_DATA_DIR", "data");
            String owner = System.getenv("MIGRATE_OWNER");
            String password = System.getenv("MIGRATE_DEFAULT_PASSWORD");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--data".equals(arg) && i + 1 < args.length) {
                    data = args[++i];
                } else if ("--owner".equals(arg) && i + 1 < args.length) {
                    owner = args[++i];
                } else if ("--password".equals(arg) && i + 1 < args.length) {
                    password = args[++i];
                } else if (!arg.startsWith("--")) {
                    data = arg;
                }
            }
            return new CliOptions(Path.of(data), owner, password);
        }
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
