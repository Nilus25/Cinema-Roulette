package com.imdbselector.domain.valueobject;

import com.imdbselector.domain.query.QueryExpression;
import com.imdbselector.domain.query.QueryParser;

import java.util.Collections;
import java.util.EnumSet;
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
        Integer minRuntime,
        Integer maxRuntime,
        Integer minVotes,
        Integer maxVotes,
        Set<Genre> genres,
        Set<MediaType> mediaTypes,
        List<String> keywords,
        boolean excludeSeen,
        String query,
        String quickSearch,
        QueryExpression compiledQuery
) {
    public FilterCriteria {
        if (minRating != null && maxRating != null && minRating.compareTo(maxRating) > 0) {
            throw new IllegalArgumentException("minRating must be <= maxRating");
        }
        if (minYear != null && maxYear != null && minYear.compareTo(maxYear) > 0) {
            throw new IllegalArgumentException("minYear must be <= maxYear");
        }
        if (minRuntime != null && minRuntime < 0) {
            throw new IllegalArgumentException("minRuntime must be >= 0");
        }
        if (maxRuntime != null && maxRuntime < 0) {
            throw new IllegalArgumentException("maxRuntime must be >= 0");
        }
        if (minRuntime != null && maxRuntime != null && minRuntime > maxRuntime) {
            throw new IllegalArgumentException("minRuntime must be <= maxRuntime");
        }
        if (minVotes != null && minVotes < 0) {
            throw new IllegalArgumentException("minVotes must be >= 0");
        }
        if (maxVotes != null && maxVotes < 0) {
            throw new IllegalArgumentException("maxVotes must be >= 0");
        }
        if (minVotes != null && maxVotes != null && minVotes > maxVotes) {
            throw new IllegalArgumentException("minVotes must be <= maxVotes");
        }
        genres = (genres == null || genres.isEmpty())
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(genres));
        mediaTypes = (mediaTypes == null || mediaTypes.isEmpty())
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(mediaTypes));
        keywords = keywords == null ? List.of() : keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .toList();
        query = (query == null || query.isBlank()) ? null : query.trim();
        quickSearch = (quickSearch == null || quickSearch.isBlank()) ? null : quickSearch.trim();
        if (compiledQuery == null && query != null) {
            compiledQuery = QueryParser.parse(query);
        }
    }

    public FilterCriteria(
            Rating minRating,
            Rating maxRating,
            ReleaseYear minYear,
            ReleaseYear maxYear,
            Integer minRuntime,
            Integer maxRuntime,
            Integer minVotes,
            Integer maxVotes,
            Set<Genre> genres,
            Set<MediaType> mediaTypes,
            List<String> keywords,
            boolean excludeSeen,
            String query,
            String quickSearch
    ) {
        this(
                minRating,
                maxRating,
                minYear,
                maxYear,
                minRuntime,
                maxRuntime,
                minVotes,
                maxVotes,
                genres,
                mediaTypes,
                keywords,
                excludeSeen,
                query,
                quickSearch,
                null
        );
    }

    public FilterCriteria(
            Rating minRating,
            Rating maxRating,
            ReleaseYear minYear,
            ReleaseYear maxYear,
            Integer minRuntime,
            Integer maxRuntime,
            Integer minVotes,
            Integer maxVotes,
            Set<Genre> genres,
            Set<MediaType> mediaTypes,
            List<String> keywords,
            boolean excludeSeen,
            String query
    ) {
        this(
                minRating,
                maxRating,
                minYear,
                maxYear,
                minRuntime,
                maxRuntime,
                minVotes,
                maxVotes,
                genres,
                mediaTypes,
                keywords,
                excludeSeen,
                query,
                null,
                null
        );
    }

    public FilterCriteria(
            Rating minRating,
            Rating maxRating,
            ReleaseYear minYear,
            ReleaseYear maxYear,
            Set<Genre> genres,
            Set<MediaType> mediaTypes,
            List<String> keywords,
            boolean excludeSeen
    ) {
        this(
                minRating,
                maxRating,
                minYear,
                maxYear,
                null,
                null,
                null,
                null,
                genres,
                mediaTypes,
                keywords,
                excludeSeen,
                null,
                null,
                null
        );
    }

    public static FilterCriteria unfiltered() {
        return new FilterCriteria(null, null, null, null, Set.of(), Set.of(), List.of(), false);
    }
}
