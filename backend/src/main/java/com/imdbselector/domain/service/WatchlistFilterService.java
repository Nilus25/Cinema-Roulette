package com.imdbselector.domain.service;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.FilterCriteria;

import java.util.List;
import java.util.Locale;

/**
 * Pure domain service that filters MediaItems according to FilterCriteria.
 */
public final class WatchlistFilterService {

    public List<MediaItem> filter(List<MediaItem> items, FilterCriteria criteria) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        FilterCriteria effective = criteria == null ? FilterCriteria.unfiltered() : criteria;

        return items.stream()
                .filter(item -> matchesRating(item, effective))
                .filter(item -> matchesYear(item, effective))
                .filter(item -> matchesGenres(item, effective))
                .filter(item -> matchesMediaTypes(item, effective))
                .filter(item -> matchesKeywords(item, effective))
                .toList();
    }

    private boolean matchesRating(MediaItem item, FilterCriteria criteria) {
        if (criteria.minRating() == null && criteria.maxRating() == null) {
            return true;
        }
        if (item.rating() == null) {
            return false;
        }
        if (criteria.minRating() != null && item.rating().compareTo(criteria.minRating()) < 0) {
            return false;
        }
        return criteria.maxRating() == null || item.rating().compareTo(criteria.maxRating()) <= 0;
    }

    private boolean matchesYear(MediaItem item, FilterCriteria criteria) {
        if (criteria.minYear() == null && criteria.maxYear() == null) {
            return true;
        }
        if (item.releaseYear() == null) {
            return false;
        }
        if (criteria.minYear() != null && item.releaseYear().compareTo(criteria.minYear()) < 0) {
            return false;
        }
        return criteria.maxYear() == null || item.releaseYear().compareTo(criteria.maxYear()) <= 0;
    }

    private boolean matchesGenres(MediaItem item, FilterCriteria criteria) {
        if (criteria.genres().isEmpty()) {
            return true;
        }
        return item.genres().stream().anyMatch(criteria.genres()::contains);
    }

    private boolean matchesMediaTypes(MediaItem item, FilterCriteria criteria) {
        if (criteria.mediaTypes().isEmpty()) {
            return true;
        }
        return criteria.mediaTypes().contains(item.mediaType());
    }

    private boolean matchesKeywords(MediaItem item, FilterCriteria criteria) {
        if (criteria.keywords().isEmpty()) {
            return true;
        }
        String titleLower = item.title().toLowerCase(Locale.ROOT);
        String directorLower = item.director().toLowerCase(Locale.ROOT);
        return criteria.keywords().stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .anyMatch(k -> titleLower.contains(k) || directorLower.contains(k));
    }
}
