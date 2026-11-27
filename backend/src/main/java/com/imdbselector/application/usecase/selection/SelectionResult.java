package com.imdbselector.application.usecase.selection;

import com.imdbselector.domain.model.MediaItem;

import java.util.List;

public record SelectionResult(MediaItem picked, List<MediaItem> reelCandidates) {
}
