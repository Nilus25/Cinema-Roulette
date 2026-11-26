package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.CsvWatchlistParserPort;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class WatchlistService {
    private final WatchlistRepository watchlistRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;
    private final CsvWatchlistParserPort csvParser;
    private final WatchlistFilterService filterService;
    private final Clock clock;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            MediaItemRepository mediaItemRepository,
            UserRepository userRepository,
            CsvWatchlistParserPort csvParser,
            WatchlistFilterService filterService,
            Clock clock
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.csvParser = Objects.requireNonNull(csvParser);
        this.filterService = Objects.requireNonNull(filterService);
        this.clock = Objects.requireNonNull(clock);
    }

    public Watchlist importFromCsv(String name, InputStream csvStream) {
        String trimmed = name == null ? "" : name.trim();
        watchlistRepository.findByName(trimmed).ifPresent(existing -> {
            throw new DuplicateResourceException("Watchlist already exists: " + trimmed);
        });
        List<MediaItem> parsed = csvParser.parse(csvStream);
        mediaItemRepository.upsertAll(parsed);
        List<ImdbId> ids = parsed.stream().map(MediaItem::imdbId).toList();
        return watchlistRepository.save(Watchlist.create(trimmed, ids));
    }

    public Watchlist updateFromCsv(WatchlistId watchlistId, InputStream csvStream) {
        Watchlist watchlist = findWatchlistOrThrow(watchlistId);
        List<MediaItem> parsed = csvParser.parse(csvStream);
        mediaItemRepository.upsertAll(parsed);
        List<ImdbId> ids = parsed.stream().map(MediaItem::imdbId).toList();
        watchlist.replaceItems(ids, clock.instant());
        return watchlistRepository.save(watchlist);
    }

    public List<Watchlist> listWatchlists() {
        return watchlistRepository.findAll();
    }

    public List<WatchlistItemView> getWatchlistItems(WatchlistId watchlistId, FilterCriteria criteria, UserId userId) {
        Watchlist watchlist = findWatchlistOrThrow(watchlistId);
        List<MediaItem> items = mediaItemRepository.findByIds(watchlist.mediaItemIds());
        List<MediaItem> filtered = filterService.filter(items, criteria);

        Set<ImdbId> seenIds = userId == null
                ? Set.of()
                : userRepository.findById(userId).map(u -> u.seenMediaIds()).orElse(Set.of());

        return filtered.stream()
                .filter(item -> criteria == null || !criteria.excludeSeen() || !seenIds.contains(item.imdbId()))
                .map(item -> new WatchlistItemView(item, seenIds.contains(item.imdbId())))
                .toList();
    }

    public void deleteWatchlist(WatchlistId watchlistId) {
        findWatchlistOrThrow(watchlistId);
        watchlistRepository.deleteById(watchlistId);
    }

    private Watchlist findWatchlistOrThrow(WatchlistId watchlistId) {
        return watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found: " + watchlistId.value()));
    }

    public record WatchlistItemView(MediaItem item, boolean seen) {
    }
}
