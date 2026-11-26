package com.imdbselector.domain.valueobject;

import java.util.List;
import java.util.Set;

/**
 * Immutable filtering criteria for selecting or browsing MediaItems in a Watchlist.
 */
public record FilterCriteria(
        Rating minRating,
        Rating maxRating,
        ReleaseYear minYear,
        ReleaseYear maxYear,
        Set<Genre> genres,
        Set<MediaType> mediaTypes,
        List<String> keywords,
        boolean excludeSeen
) {
    public FilterCriteria {
        genres = genres == null ? Set.of() : Set.copyOf(genres);
        mediaTypes = mediaTypes == null ? Set.of() : Set.copyOf(mediaTypes);
        keywords = keywords == null ? List.of() : keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .toList();
    }

    public static FilterCriteria unfiltered() {
        return new FilterCriteria(null, null, null, null, Set.of(), Set.of(), List.of(), false);
    }
}
