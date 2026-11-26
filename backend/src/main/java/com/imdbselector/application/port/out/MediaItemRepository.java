package com.imdbselector.application.port.out;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.ImdbId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaItemRepository {
    void upsertAll(List<MediaItem> items);

    Optional<MediaItem> findById(ImdbId imdbId);

    List<MediaItem> findByIds(Collection<ImdbId> imdbIds);
}
