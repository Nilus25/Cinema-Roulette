package com.imdbselector.application.usecase.watchlist;

import com.imdbselector.application.port.out.CsvWatchlistParserPort;
import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;

import java.io.InputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class UpdateWatchlistUseCase {
    private final WatchlistRepository watchlistRepository;
    private final MediaItemRepository mediaItemRepository;
    private final UserRepository userRepository;
    private final CsvWatchlistParserPort csvParser;
    private final TransactionPort transactionPort;
    private final Clock clock;

    public UpdateWatchlistUseCase(
            WatchlistRepository watchlistRepository,
            MediaItemRepository mediaItemRepository,
            UserRepository userRepository,
            CsvWatchlistParserPort csvParser,
            TransactionPort transactionPort,
            Clock clock
    ) {
        this.watchlistRepository = Objects.requireNonNull(watchlistRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.csvParser = Objects.requireNonNull(csvParser);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.clock = Objects.requireNonNull(clock);
    }

    public Watchlist execute(UserId actorId, WatchlistId watchlistId, InputStream csvStream) {
        return execute(actorId, watchlistId, csvStream, Set.of(), Set.of(), Set.of());
    }

    public Watchlist execute(
            UserId actorId,
            WatchlistId watchlistId,
            InputStream csvStream,
            Set<ImdbId> excludeAddedIds,
            Set<ImdbId> keepRemovedIds,
            Set<ImdbId> markSeenIds
    ) {
        List<MediaItem> parsed = csvParser.parse(csvStream);
        Set<ImdbId> incomingIds = new LinkedHashSet<>(parsed.stream().map(MediaItem::imdbId).toList());
        Set<ImdbId> safeExclude = excludeAddedIds == null ? Set.of() : excludeAddedIds;
        Set<ImdbId> safeKeep = keepRemovedIds == null ? Set.of() : keepRemovedIds;
        Set<ImdbId> safeMarkSeen = markSeenIds == null ? Set.of() : markSeenIds;

        return transactionPort.inTransaction(() -> {
            Watchlist watchlist = WatchlistAccess.findOwnedOrThrow(watchlistRepository, actorId, watchlistId);
            Set<ImdbId> currentIds = new LinkedHashSet<>(watchlist.mediaItemIds());

            List<ImdbId> finalIds = new ArrayList<>();
            for (ImdbId existingId : watchlist.mediaItemIds()) {
                if (incomingIds.contains(existingId) || safeKeep.contains(existingId)) {
                    finalIds.add(existingId);
                }
            }
            for (ImdbId incomingId : incomingIds) {
                if (!currentIds.contains(incomingId) && !safeExclude.contains(incomingId)) {
                    finalIds.add(incomingId);
                }
            }

            mediaItemRepository.upsertAll(parsed);
            watchlist.replaceItems(finalIds, clock.instant());
            Watchlist saved = watchlistRepository.save(watchlist);

            if (!safeMarkSeen.isEmpty()) {
                User user = userRepository.findById(actorId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorId.value()));
                for (ImdbId seenId : safeMarkSeen) {
                    if (incomingIds.contains(seenId) || currentIds.contains(seenId)) {
                        user.markAsSeen(seenId);
                    }
                }
                userRepository.save(user);
            }

            return saved;
        });
    }
}
