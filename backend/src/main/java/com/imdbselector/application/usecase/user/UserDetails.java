package com.imdbselector.application.usecase.user;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;

import java.util.List;
import java.util.Optional;

public record UserDetails(User user, MediaItem lastGeneratedItem, List<MediaItem> seenItems) {
    public Optional<MediaItem> unseenLastGeneratedPrompt() {
        if (lastGeneratedItem != null && user.isLastGeneratedUnseen()) {
            return Optional.of(lastGeneratedItem);
        }
        return Optional.empty();
    }
}
