package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.CsvWatchlistParserPort;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistDiff;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public final class PreviewWatchlistUpdateUseCase {
    private final WatchlistRepository watchlistRepository;
    private final MediaItemRepository mediaItemRepository;
    private final CsvWatchlistParserPort csvParser;

    public PreviewWatchlistUpdateUseCase(
            WatchlistRepository watchlistRepository,
            MediaItemRepository mediaItemRepository,
            CsvWatchlistParserPort csvParser
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.csvParser = Objects.requireNonNull(csvParser);
    }

    public WatchlistDiff execute(UserId actorId, WatchlistId watchlistId, InputStream csvStream) {
        Watchlist watchlist = WatchlistAccess.findOwnedOrThrow(watchlistRepository, actorId, watchlistId);
        List<MediaItem> incoming = csvParser.parse(csvStream);
        List<MediaItem> current = mediaItemRepository.findByIds(watchlist.mediaItemIds());
        return watchlist.diffAgainst(current, incoming);
    }
}
