package com.imdbselector.domain.model;

import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical media catalog entry keyed by its IMDB identifier.
 */
public record MediaItem(
        ImdbId imdbId,
        String title,
        String originalTitle,
        String director,
        ReleaseYear releaseYear,
        Rating rating,
        Integer runtimeMinutes,
        Integer numVotes,
        MediaType mediaType,
        String imdbUrl,
        Set<Genre> genres,
        MetadataSource source
) {
    public MediaItem {
        Objects.requireNonNull(imdbId, "imdbId must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (runtimeMinutes != null && runtimeMinutes < 0) {
            throw new IllegalArgumentException("runtimeMinutes must be >= 0");
        }
        if (numVotes != null && numVotes < 0) {
            throw new IllegalArgumentException("numVotes must be >= 0");
        }
        title = title.trim();
        originalTitle = (originalTitle == null || originalTitle.isBlank()) ? null : originalTitle.trim();
        director = director == null ? "" : director.trim();
        mediaType = mediaType == null ? MediaType.OTHER : mediaType;
        imdbUrl = imdbUrl == null || imdbUrl.isBlank()
                ? "https://www.imdb.com/title/" + imdbId.value() + "/"
                : imdbUrl.trim();
        genres = (genres == null || genres.isEmpty())
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(genres));
        source = source == null ? MetadataSource.CSV : source;
    }

    public MediaItem(
            ImdbId imdbId,
            String title,
            String director,
            ReleaseYear releaseYear,
            Rating rating,
            MediaType mediaType,
            String imdbUrl,
            Set<Genre> genres,
            MetadataSource source
    ) {
        this(imdbId, title, null, director, releaseYear, rating, null, null, mediaType, imdbUrl, genres, source);
    }

    public boolean matches(FilterCriteria criteria) {
        return matches(criteria, false);
    }

    public boolean matches(FilterCriteria criteria, boolean seen) {
        FilterCriteria effective = criteria == null ? FilterCriteria.unfiltered() : criteria;
        return matchesRating(effective)
                && matchesYear(effective)
                && matchesRuntime(effective)
                && matchesVotes(effective)
                && matchesGenres(effective)
                && matchesMediaTypes(effective)
                && matchesKeywords(effective)
                && matchesQuickSearch(effective)
                && matchesQuery(effective, seen);
    }

    private boolean matchesRating(FilterCriteria criteria) {
        if (criteria.minRating() == null && criteria.maxRating() == null) {
            return true;
        }
        if (rating == null) {
            return false;
        }
        if (criteria.minRating() != null && rating.compareTo(criteria.minRating()) < 0) {
            return false;
        }
        return criteria.maxRating() == null || rating.compareTo(criteria.maxRating()) <= 0;
    }

    private boolean matchesYear(FilterCriteria criteria) {
        if (criteria.minYear() == null && criteria.maxYear() == null) {
            return true;
        }
        if (releaseYear == null) {
            return false;
        }
        if (criteria.minYear() != null && releaseYear.compareTo(criteria.minYear()) < 0) {
            return false;
        }
        return criteria.maxYear() == null || releaseYear.compareTo(criteria.maxYear()) <= 0;
    }

    private boolean matchesRuntime(FilterCriteria criteria) {
        if (criteria.minRuntime() == null && criteria.maxRuntime() == null) {
            return true;
        }
        if (runtimeMinutes == null) {
            return false;
        }
        if (criteria.minRuntime() != null && runtimeMinutes < criteria.minRuntime()) {
            return false;
        }
        return criteria.maxRuntime() == null || runtimeMinutes <= criteria.maxRuntime();
    }

    private boolean matchesVotes(FilterCriteria criteria) {
        if (criteria.minVotes() == null && criteria.maxVotes() == null) {
            return true;
        }
        if (numVotes == null) {
            return false;
        }
        if (criteria.minVotes() != null && numVotes < criteria.minVotes()) {
            return false;
        }
        return criteria.maxVotes() == null || numVotes <= criteria.maxVotes();
    }

    private boolean matchesGenres(FilterCriteria criteria) {
        return criteria.genres().isEmpty() || genres.stream().anyMatch(criteria.genres()::contains);
    }

    private boolean matchesMediaTypes(FilterCriteria criteria) {
        return criteria.mediaTypes().isEmpty() || criteria.mediaTypes().contains(mediaType);
    }

    private boolean matchesKeywords(FilterCriteria criteria) {
        if (criteria.keywords().isEmpty()) {
            return true;
        }
        return criteria.keywords().stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .anyMatch(this::containsLowerNeedle);
    }

    private boolean matchesQuickSearch(FilterCriteria criteria) {
        if (criteria.quickSearch() == null || criteria.quickSearch().isBlank()) {
            return true;
        }
        return containsLowerNeedle(criteria.quickSearch().toLowerCase(Locale.ROOT));
    }

    private boolean containsLowerNeedle(String needleLower) {
        return title.toLowerCase(Locale.ROOT).contains(needleLower)
                || (originalTitle != null && originalTitle.toLowerCase(Locale.ROOT).contains(needleLower))
                || director.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private boolean matchesQuery(FilterCriteria criteria, boolean seen) {
        return criteria.compiledQuery() == null || criteria.compiledQuery().evaluate(this, seen);
    }
}
