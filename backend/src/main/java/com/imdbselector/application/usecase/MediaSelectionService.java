package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.service.RandomMediaSelector;
import com.imdbselector.domain.service.SelectionMode;
import com.imdbselector.domain.service.WatchlistFilterService;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class MediaSelectionService {
    private final WatchlistRepository watchlistRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;
    private final WatchlistFilterService filterService;
    private final RandomMediaSelector selector;
    private final Clock clock;

    public MediaSelectionService(
            WatchlistRepository watchlistRepository,
            MediaItemRepository mediaItemRepository,
            UserRepository userRepository,
            WatchlistFilterService filterService,
            RandomMediaSelector selector,
            Clock clock
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.filterService = Objects.requireNonNull(filterService);
        this.selector = Objects.requireNonNull(selector);
        this.clock = Objects.requireNonNull(clock);
    }

    public MediaItem selectRandom(WatchlistId watchlistId, UserId userId, FilterCriteria criteria) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found: " + watchlistId.value()));

        User user = userId == null ? null : userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));

        List<MediaItem> allItems = mediaItemRepository.findByIds(watchlist.mediaItemIds());
        List<MediaItem> filtered = filterService.filter(allItems, criteria);

        SelectionMode mode = (criteria != null && criteria.excludeSeen() && user != null)
                ? new SelectionMode.NotSeen(user.seenMediaIds())
                : new SelectionMode.Random();

        MediaItem picked = selector.select(filtered, mode);

        if (user != null) {
            user.recordGenerated(picked.imdbId(), clock.instant());
            userRepository.save(user);
        }

        return picked;
    }
}
