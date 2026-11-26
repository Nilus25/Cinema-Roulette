package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.CsvWatchlistParserPort;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ForbiddenException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class WatchlistService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;
    private final CsvWatchlistParserPort csvParser;
    private final WatchlistFilterService filterService;
    private final Clock clock;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            MediaItemRepository mediaItemRepository,
            UserRepository userRepository,
            CsvWatchlistParserPort csvParser,
            WatchlistFilterService filterService,
            Clock clock
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.csvParser = Objects.requireNonNull(csvParser);
        this.filterService = Objects.requireNonNull(filterService);
        this.clock = Objects.requireNonNull(clock);
    }

    public Watchlist importFromCsv(UserId ownerId, String name, InputStream csvStream) {
        String trimmed = name == null ? "" : name.trim();
        watchlistRepository.findByOwnerAndName(ownerId, trimmed).ifPresent(existing -> {
            throw new DuplicateResourceException("You already have a watchlist named: " + trimmed);
        });
        List<MediaItem> parsed = csvParser.parse(csvStream);
        mediaItemRepository.upsertAll(parsed);
        List<ImdbId> ids = parsed.stream().map(MediaItem::imdbId).toList();
        return watchlistRepository.save(Watchlist.create(ownerId, trimmed, ids));
    }

    public Watchlist updateFromCsv(UserId actorId, WatchlistId watchlistId, InputStream csvStream) {
        Watchlist watchlist = findOwnedWatchlistOrThrow(actorId, watchlistId);
        List<MediaItem> parsed = csvParser.parse(csvStream);
        mediaItemRepository.upsertAll(parsed);
        List<ImdbId> ids = parsed.stream().map(MediaItem::imdbId).toList();
        watchlist.replaceItems(ids, clock.instant());
        return watchlistRepository.save(watchlist);
    }

    public Watchlist changeVisibility(UserId actorId, WatchlistId watchlistId, WatchlistVisibility visibility) {
        Watchlist watchlist = findOwnedWatchlistOrThrow(actorId, watchlistId);
        String shareCode = switch (visibility) {
            case PRIVATE -> null;
            case SHARED, PUBLIC -> watchlist.shareCode() != null ? watchlist.shareCode() : generateShareCode();
        };
        watchlist.changeVisibility(visibility, shareCode, clock.instant());
        return watchlistRepository.save(watchlist);
    }

    public void deleteWatchlist(UserId actorId, WatchlistId watchlistId) {
        findOwnedWatchlistOrThrow(actorId, watchlistId);
        watchlistRepository.deleteById(watchlistId);
    }

    public List<WatchlistSummary> listAccessibleWatchlists(UserId userId) {
        List<Watchlist> owned = watchlistRepository.findByOwner(userId);
        List<WatchlistId> subIds = subscriptionRepository.findSubscribedWatchlistIds(userId);
        List<Watchlist> subscribed = watchlistRepository.findByIds(subIds);

        Map<UserId, String> ownerNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::id, User::name, (a, b) -> a));

        List<WatchlistSummary> result = new ArrayList<>();
        for (Watchlist w : owned) {
            result.add(new WatchlistSummary(w, true, ownerNames.getOrDefault(w.ownerUserId(), "You")));
        }
        for (Watchlist w : subscribed) {
            if (!w.isOwnedBy(userId)) {
                result.add(new WatchlistSummary(w, false, ownerNames.getOrDefault(w.ownerUserId(), "Unknown")));
            }
        }
        return result;
    }

    public List<WatchlistSummary> listPublicWatchlists(UserId userId) {
        Map<UserId, String> ownerNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::id, User::name, (a, b) -> a));
        return watchlistRepository.findPublicWatchlists().stream()
                .map(w -> new WatchlistSummary(w, w.isOwnedBy(userId), ownerNames.getOrDefault(w.ownerUserId(), "Unknown")))
                .toList();
    }

    public void subscribeByShareCode(UserId userId, String shareCode) {
        if (shareCode == null || shareCode.isBlank()) {
            throw new IllegalArgumentException("Share code must not be blank");
        }
        Watchlist watchlist = watchlistRepository.findByShareCode(shareCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No watchlist found for share code"));
        if (watchlist.isOwnedBy(userId)) {
            throw new IllegalArgumentException("You already own this watchlist");
        }
        if (watchlist.visibility() == WatchlistVisibility.PRIVATE) {
            throw new ForbiddenException("This watchlist is private");
        }
        subscriptionRepository.subscribe(userId, watchlist.id());
    }

    public void subscribePublic(UserId userId, WatchlistId watchlistId) {
        Watchlist watchlist = findWatchlistOrThrow(watchlistId);
        if (watchlist.isOwnedBy(userId)) {
            throw new IllegalArgumentException("You already own this watchlist");
        }
        if (watchlist.visibility() != WatchlistVisibility.PUBLIC) {
            throw new ForbiddenException("Only public watchlists can be subscribed without a share code");
        }
        subscriptionRepository.subscribe(userId, watchlist.id());
    }

    public void unsubscribe(UserId userId, WatchlistId watchlistId) {
        subscriptionRepository.unsubscribe(userId, watchlistId);
    }

    public List<WatchlistItemView> getWatchlistItems(UserId actorId, WatchlistId watchlistId, FilterCriteria criteria) {
        Watchlist watchlist = findViewableWatchlistOrThrow(actorId, watchlistId);
        List<MediaItem> items = mediaItemRepository.findByIds(watchlist.mediaItemIds());
        List<MediaItem> filtered = filterService.filter(items, criteria);

        Set<ImdbId> seenIds = userRepository.findById(actorId)
                .map(User::seenMediaIds)
                .orElse(Set.of());

        return filtered.stream()
                .filter(item -> criteria == null || !criteria.excludeSeen() || !seenIds.contains(item.imdbId()))
                .map(item -> new WatchlistItemView(item, seenIds.contains(item.imdbId())))
                .toList();
    }

    private Watchlist findOwnedWatchlistOrThrow(UserId actorId, WatchlistId watchlistId) {
        Watchlist watchlist = findWatchlistOrThrow(watchlistId);
        if (!watchlist.isOwnedBy(actorId)) {
            throw new ForbiddenException("Only the watchlist owner can modify this watchlist");
        }
        return watchlist;
    }

    private Watchlist findViewableWatchlistOrThrow(UserId actorId, WatchlistId watchlistId) {
        Watchlist watchlist = findWatchlistOrThrow(watchlistId);
        boolean subscribed = subscriptionRepository.isSubscribed(actorId, watchlistId);
        if (!watchlist.canBeViewedBy(actorId, subscribed)) {
            throw new ForbiddenException("You do not have access to this watchlist");
        }
        return watchlist;
    }

    private Watchlist findWatchlistOrThrow(WatchlistId watchlistId) {
        return watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found: " + watchlistId.value()));
    }

    private static String generateShareCode() {
        byte[] bytes = new byte[5];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record WatchlistItemView(MediaItem item, boolean seen) {
    }

    public record WatchlistSummary(Watchlist watchlist, boolean owned, String ownerName) {
    }
}
