package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.selection.SelectionResult;
import com.imdbselector.application.usecase.user.UserDetails;
import com.imdbselector.application.usecase.user.UserExportData;
import com.imdbselector.application.usecase.watchlist.PagedWatchlistItems;
import com.imdbselector.application.usecase.watchlist.WatchlistItemView;
import com.imdbselector.application.usecase.watchlist.WatchlistSummary;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.WatchlistDiff;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record AuthRequest(String username, String password) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    public record UpdatePreferencesRequest(String queryFlavor) {
    }

    public record SubscribeCodeRequest(String shareCode) {
    }

    public record VisibilityRequest(String visibility) {
    }

    public record UserResponse(
            UUID id,
            String name,
            String lastGeneratedImdbId,
            Instant lastGeneratedAt,
            int seenCount,
            String queryFlavor
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id().value(),
                    user.name(),
                    user.lastGeneratedImdbId() == null ? null : user.lastGeneratedImdbId().value(),
                    user.lastGeneratedAt(),
                    user.seenMediaIds().size(),
                    user.queryFlavor().name()
            );
        }
    }

    public record UserDetailsResponse(
            UUID id,
            String name,
            String queryFlavor,
            MediaItemResponse unseenLastGeneratedPrompt,
            List<MediaItemResponse> seenItems
    ) {
        public static UserDetailsResponse from(UserDetails details) {
            return new UserDetailsResponse(
                    details.user().id().value(),
                    details.user().name(),
                    details.user().queryFlavor().name(),
                    details.unseenLastGeneratedPrompt().map(MediaItemResponse::from).orElse(null),
                    details.seenItems().stream().map(MediaItemResponse::from).toList()
            );
        }
    }

    public record UserExportResponse(
            Instant exportedAt,
            String username,
            Instant createdAt,
            String lastGeneratedImdbId,
            Instant lastGeneratedAt,
            List<MediaItemResponse> seenMovies,
            List<OwnedWatchlistExportResponse> ownedWatchlists,
            List<SubscribedWatchlistSummaryResponse> subscriptions
    ) {
        public static UserExportResponse from(UserExportData data) {
            User user = data.user();
            return new UserExportResponse(
                    data.exportedAt(),
                    user.name(),
                    user.createdAt(),
                    user.lastGeneratedImdbId() == null ? null : user.lastGeneratedImdbId().value(),
                    user.lastGeneratedAt(),
                    data.seenMovies().stream().map(MediaItemResponse::from).toList(),
                    data.ownedWatchlists().stream().map(OwnedWatchlistExportResponse::from).toList(),
                    data.subscriptions().stream().map(SubscribedWatchlistSummaryResponse::from).toList()
            );
        }
    }

    public record OwnedWatchlistExportResponse(
            UUID id,
            String name,
            String visibility,
            String shareCode,
            Instant createdAt,
            Instant updatedAt,
            List<MediaItemResponse> items
    ) {
        public static OwnedWatchlistExportResponse from(UserExportData.OwnedWatchlistExport export) {
            Watchlist wl = export.watchlist();
            return new OwnedWatchlistExportResponse(
                    wl.id().value(),
                    wl.name(),
                    wl.visibility().name(),
                    wl.shareCode(),
                    wl.createdAt(),
                    wl.updatedAt(),
                    export.items().stream().map(MediaItemResponse::from).toList()
            );
        }
    }

    public record SubscribedWatchlistSummaryResponse(
            UUID id,
            String name,
            String ownerName,
            String visibility,
            int itemCount,
            Instant updatedAt
    ) {
        public static SubscribedWatchlistSummaryResponse from(UserExportData.SubscribedWatchlistSummary summary) {
            Watchlist wl = summary.watchlist();
            return new SubscribedWatchlistSummaryResponse(
                    wl.id().value(),
                    wl.name(),
                    summary.ownerName(),
                    wl.visibility().name(),
                    wl.mediaItemIds().size(),
                    wl.updatedAt()
            );
        }
    }

    public record WatchlistResponse(
            UUID id,
            String name,
            int itemCount,
            Instant updatedAt,
            boolean owned,
            String ownerName,
            String visibility,
            String shareCode
    ) {
        public static WatchlistResponse from(WatchlistSummary summary) {
            Watchlist wl = summary.watchlist();
            return new WatchlistResponse(
                    wl.id().value(),
                    wl.name(),
                    wl.mediaItemIds().size(),
                    wl.updatedAt(),
                    summary.owned(),
                    summary.ownerName(),
                    wl.visibility().name(),
                    summary.owned() ? wl.shareCode() : null
            );
        }
    }

    public record MediaItemResponse(
            String imdbId,
            String title,
            String originalTitle,
            String director,
            Integer releaseYear,
            BigDecimal rating,
            Integer runtimeMinutes,
            Integer numVotes,
            String mediaType,
            String imdbUrl,
            List<String> genres
    ) {
        public static MediaItemResponse from(MediaItem item) {
            return new MediaItemResponse(
                    item.imdbId().value(),
                    item.title(),
                    item.originalTitle(),
                    item.director(),
                    item.releaseYear() == null ? null : item.releaseYear().value(),
                    item.rating() == null ? null : item.rating().value(),
                    item.runtimeMinutes(),
                    item.numVotes(),
                    item.mediaType().label(),
                    item.imdbUrl(),
                    item.genres().stream().map(Genre::label).sorted().toList()
            );
        }
    }

    public record WatchlistItemResponse(MediaItemResponse item, boolean seen) {
        public static WatchlistItemResponse from(WatchlistItemView view) {
            return new WatchlistItemResponse(MediaItemResponse.from(view.item()), view.seen());
        }
    }

    public record PagedWatchlistItemsResponse(
            List<WatchlistItemResponse> items,
            int page,
            int size,
            int totalItems,
            int totalPages,
            boolean hasMore,
            int unseenCount,
            BigDecimal avgRating,
            String topGenre
    ) {
        public static PagedWatchlistItemsResponse from(PagedWatchlistItems result) {
            return new PagedWatchlistItemsResponse(
                    result.items().stream().map(WatchlistItemResponse::from).toList(),
                    result.page(),
                    result.size(),
                    result.totalItems(),
                    result.totalPages(),
                    result.hasMore(),
                    result.unseenCount(),
                    result.avgRating(),
                    result.topGenre()
            );
        }
    }

    public record WatchlistUpdatePreviewResponse(
            List<MediaItemResponse> added,
            List<MediaItemResponse> removed
    ) {
        public static WatchlistUpdatePreviewResponse from(WatchlistDiff diff) {
            return new WatchlistUpdatePreviewResponse(
                    diff.added().stream().map(MediaItemResponse::from).toList(),
                    diff.removed().stream().map(MediaItemResponse::from).toList()
            );
        }
    }

    public record SelectRequest(
            BigDecimal minRating,
            BigDecimal maxRating,
            Integer minYear,
            Integer maxYear,
            Integer minRuntime,
            Integer maxRuntime,
            Integer minVotes,
            Integer maxVotes,
            List<String> genres,
            List<String> mediaTypes,
            List<String> keywords,
            boolean excludeSeen,
            String query
    ) {
        public static SelectRequest empty() {
            return new SelectRequest(null, null, null, null, null, null, null, null, null, null, null, false, null);
        }

        public FilterCriteria toCriteria() {
            Set<Genre> genreSet = genres == null ? Set.of() : genres.stream().map(Genre::fromLabel).collect(Collectors.toSet());
            Set<MediaType> typeSet = mediaTypes == null ? Set.of() : mediaTypes.stream().map(MediaType::fromLabel).collect(Collectors.toSet());
            return new FilterCriteria(
                    minRating == null ? null : new Rating(minRating),
                    maxRating == null ? null : new Rating(maxRating),
                    minYear == null ? null : new ReleaseYear(minYear),
                    maxYear == null ? null : new ReleaseYear(maxYear),
                    minRuntime,
                    maxRuntime,
                    minVotes,
                    maxVotes,
                    genreSet,
                    typeSet,
                    keywords,
                    excludeSeen,
                    query
            );
        }
    }

    public record SelectionResponse(
            MediaItemResponse picked,
            List<MediaItemResponse> reelCandidates
    ) {
        public static SelectionResponse from(SelectionResult result) {
            return new SelectionResponse(
                    MediaItemResponse.from(result.picked()),
                    result.reelCandidates().stream().map(MediaItemResponse::from).toList()
            );
        }
    }

    /**
     * RFC 9457 Problem Details payload for HTTP error responses.
     */
    public record ProblemDetail(
            String type,
            String title,
            int status,
            String detail,
            String code,
            String traceId
    ) {
    }
}
