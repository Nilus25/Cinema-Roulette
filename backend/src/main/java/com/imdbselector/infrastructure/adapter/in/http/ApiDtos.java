package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.application.usecase.UserService;
import com.imdbselector.application.usecase.WatchlistService;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record CreateUserRequest(String name) {
    }

    public record UserResponse(
            UUID id,
            String name,
            String lastGeneratedImdbId,
            Instant lastGeneratedAt,
            int seenCount
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.id().value(),
                    user.name(),
                    user.lastGeneratedImdbId() == null ? null : user.lastGeneratedImdbId().value(),
                    user.lastGeneratedAt(),
                    user.seenMediaIds().size()
            );
        }
    }

    public record UserDetailsResponse(
            UUID id,
            String name,
            MediaItemResponse unseenLastGeneratedPrompt,
            List<MediaItemResponse> seenItems
    ) {
        public static UserDetailsResponse from(UserService.UserDetails details) {
            return new UserDetailsResponse(
                    details.user().id().value(),
                    details.user().name(),
                    details.unseenLastGeneratedPrompt().map(MediaItemResponse::from).orElse(null),
                    details.seenItems().stream().map(MediaItemResponse::from).toList()
            );
        }
    }

    public record WatchlistResponse(
            UUID id,
            String name,
            int itemCount,
            Instant updatedAt
    ) {
        public static WatchlistResponse from(Watchlist wl) {
            return new WatchlistResponse(
                    wl.id().value(),
                    wl.name(),
                    wl.mediaItemIds().size(),
                    wl.updatedAt()
            );
        }
    }

    public record MediaItemResponse(
            String imdbId,
            String title,
            String director,
            Integer releaseYear,
            BigDecimal rating,
            String mediaType,
            String imdbUrl,
            List<String> genres
    ) {
        public static MediaItemResponse from(MediaItem item) {
            return new MediaItemResponse(
                    item.imdbId().value(),
                    item.title(),
                    item.director(),
                    item.releaseYear() == null ? null : item.releaseYear().value(),
                    item.rating() == null ? null : item.rating().value(),
                    item.mediaType().label(),
                    item.imdbUrl(),
                    item.genres().stream().map(Genre::label).sorted().toList()
            );
        }
    }

    public record WatchlistItemResponse(MediaItemResponse item, boolean seen) {
        public static WatchlistItemResponse from(WatchlistService.WatchlistItemView view) {
            return new WatchlistItemResponse(MediaItemResponse.from(view.item()), view.seen());
        }
    }

    public record SelectRequest(
            UUID userId,
            BigDecimal minRating,
            BigDecimal maxRating,
            Integer minYear,
            Integer maxYear,
            List<String> genres,
            List<String> mediaTypes,
            List<String> keywords,
            boolean excludeSeen
    ) {
        public FilterCriteria toCriteria() {
            Set<Genre> genreSet = genres == null ? Set.of() : genres.stream().map(Genre::fromLabel).collect(Collectors.toSet());
            Set<MediaType> typeSet = mediaTypes == null ? Set.of() : mediaTypes.stream().map(MediaType::fromLabel).collect(Collectors.toSet());
            return new FilterCriteria(
                    minRating == null ? null : new Rating(minRating),
                    maxRating == null ? null : new Rating(maxRating),
                    minYear == null ? null : new ReleaseYear(minYear),
                    maxYear == null ? null : new ReleaseYear(maxYear),
                    genreSet,
                    typeSet,
                    keywords,
                    excludeSeen
            );
        }
    }

    public record ErrorResponse(String error, String message) {
    }
}
