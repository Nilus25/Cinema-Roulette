package com.imdbselector.application.usecase.user;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exports an authenticated user's profile, seen history, owned watchlists, and subscription summaries.
 */
public final class ExportUserDataUseCase {
    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;
    private final WatchlistSubscriptionRepository subscriptionRepository;
    private final MediaItemRepository mediaItemRepository;
    private final Clock clock;

    public ExportUserDataUseCase(
            UserRepository userRepository,
            WatchlistRepository watchlistRepository,
            WatchlistSubscriptionRepository subscriptionRepository,
            MediaItemRepository mediaItemRepository,
            Clock clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public UserExportData execute(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));

        List<Watchlist> owned = watchlistRepository.findByOwner(userId);
        List<WatchlistId> subscribedIds = subscriptionRepository.findSubscribedWatchlistIds(userId);
        List<Watchlist> subscribed = watchlistRepository.findByIds(subscribedIds).stream()
                .filter(w -> !w.isOwnedBy(userId))
                .toList();
        Set<UserId> ownerIds = subscribed.stream()
                .map(Watchlist::ownerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UserId, String> ownerNames = ownerIds.isEmpty() ? Map.of() : userRepository.findNamesByIds(ownerIds);

        Set<ImdbId> allMediaIds = new LinkedHashSet<>(user.seenMediaIds());
        for (Watchlist watchlist : owned) {
            allMediaIds.addAll(watchlist.mediaItemIds());
        }

        Map<ImdbId, MediaItem> mediaById = mediaItemRepository.findByIds(allMediaIds).stream()
                .collect(Collectors.toMap(MediaItem::imdbId, item -> item, (a, b) -> a));

        List<MediaItem> seenMovies = user.seenMediaIds().stream()
                .map(mediaById::get)
                .filter(Objects::nonNull)
                .toList();

        List<UserExportData.OwnedWatchlistExport> ownedExports = owned.stream()
                .map(w -> new UserExportData.OwnedWatchlistExport(
                        w,
                        w.mediaItemIds().stream()
                                .map(mediaById::get)
                                .filter(Objects::nonNull)
                                .toList()
                ))
                .toList();

        List<UserExportData.SubscribedWatchlistSummary> subscriptionSummaries = subscribed.stream()
                .map(w -> new UserExportData.SubscribedWatchlistSummary(
                        w,
                        ownerNames.getOrDefault(w.ownerUserId(), "Unknown")
                ))
                .toList();

        return new UserExportData(
                clock.instant(),
                user,
                seenMovies,
                ownedExports,
                subscriptionSummaries
        );
    }
}
