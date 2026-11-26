package com.imdbselector.application.usecase.user;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Objects;

public final class GetUserDetailsUseCase {
    private final UserRepository userRepository;
    private final MediaItemRepository mediaItemRepository;

    public GetUserDetailsUseCase(UserRepository userRepository, MediaItemRepository mediaItemRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
    }

    public UserDetails execute(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));
        MediaItem lastGenerated = user.lastGeneratedImdbId() == null
                ? null
                : mediaItemRepository.findById(user.lastGeneratedImdbId()).orElse(null);
        List<MediaItem> seenItems = mediaItemRepository.findByIds(user.seenMediaIds());
        return new UserDetails(user, lastGenerated, seenItems);
    }
}
