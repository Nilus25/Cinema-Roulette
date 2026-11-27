package com.imdbselector.application.usecase.user;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

        Map<ImdbId, MediaItem> byId = mediaItemRepository.findByIds(user.seenMediaIds()).stream()
                .collect(Collectors.toMap(MediaItem::imdbId, item -> item, (a, b) -> a));
        List<MediaItem> seenItems = user.seenMediaIds().stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();

        return new UserDetails(user, lastGenerated, seenItems);
    }
}
