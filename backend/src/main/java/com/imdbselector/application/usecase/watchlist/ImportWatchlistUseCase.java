package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.CsvWatchlistParserPort;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public final class ImportWatchlistUseCase {
    private final WatchlistRepository watchlistRepository;
    private final MediaItemRepository mediaItemRepository;
    private final CsvWatchlistParserPort csvParser;
    private final TransactionPort transactionPort;

    public ImportWatchlistUseCase(
            WatchlistRepository watchlistRepository,
            MediaItemRepository mediaItemRepository,
            CsvWatchlistParserPort csvParser,
            TransactionPort transactionPort
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.csvParser = Objects.requireNonNull(csvParser);
        this.transactionPort = Objects.requireNonNull(transactionPort);
    }

    public Watchlist execute(UserId ownerId, String name, InputStream csvStream) {
        String trimmed = name == null ? "" : name.trim();
        List<MediaItem> parsed = csvParser.parse(csvStream);
        List<ImdbId> ids = parsed.stream().map(MediaItem::imdbId).toList();

        return transactionPort.inTransaction(() -> {
            watchlistRepository.findByOwnerAndName(ownerId, trimmed).ifPresent(existing -> {
                throw new DuplicateResourceException("You already have a watchlist named: " + trimmed);
            });
            mediaItemRepository.upsertAll(parsed);
            return watchlistRepository.save(Watchlist.create(ownerId, trimmed, ids));
        });
    }
}
