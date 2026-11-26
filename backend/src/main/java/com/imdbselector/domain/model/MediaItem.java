package com.imdbselector.domain.model;

import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical media catalog entry keyed by its IMDB identifier.
 */
public record MediaItem(
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
    public MediaItem {
        Objects.requireNonNull(imdbId, "imdbId must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        title = title.trim();
        director = director == null ? "" : director.trim();
        mediaType = mediaType == null ? MediaType.OTHER : mediaType;
        imdbUrl = imdbUrl == null || imdbUrl.isBlank()
                ? "https://www.imdb.com/title/" + imdbId.value() + "/"
                : imdbUrl.trim();
        genres = genres == null ? Set.of() : Set.copyOf(genres);
        source = source == null ? MetadataSource.CSV : source;
    }

    /**
     * Evaluates whether this media item satisfies the given FilterCriteria.
     */
    public boolean matches(FilterCriteria criteria) {
        FilterCriteria effective = criteria == null ? FilterCriteria.unfiltered() : criteria;
        return matchesRating(effective)
                && matchesYear(effective)
                && matchesGenres(effective)
                && matchesMediaTypes(effective)
                && matchesKeywords(effective);
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
        String titleLower = title.toLowerCase(Locale.ROOT);
        String directorLower = director.toLowerCase(Locale.ROOT);
        return criteria.keywords().stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .anyMatch(k -> titleLower.contains(k) || directorLower.contains(k));
    }
}
