package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.ImdbId;

import java.util.Optional;

/**
 * Extension point for future TMDB / OMDb metadata enrichment.
 */
public interface MediaEnrichmentPort {
    Optional<MediaItem> enrich(ImdbId imdbId);
}
