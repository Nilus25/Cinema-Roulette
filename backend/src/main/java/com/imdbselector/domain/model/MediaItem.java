package com.imdbselector.domain.model;

import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;

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
}
