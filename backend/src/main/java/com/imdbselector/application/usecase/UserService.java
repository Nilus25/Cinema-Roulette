package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class UserService {
    private final UserRepository userRepository;
    private final MediaItemRepository mediaItemRepository;

    public UserService(UserRepository userRepository, MediaItemRepository mediaItemRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.mediaItemRepository = Objects.requireNonNull(mediaItemRepository);
    }

    public User createUser(String name) {
        String trimmed = name == null ? "" : name.trim();
        userRepository.findByName(trimmed).ifPresent(existing -> {
            throw new DuplicateResourceException("User already exists: " + trimmed);
        });
        return userRepository.save(User.create(trimmed));
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public UserDetails getUserDetails(UserId userId) {
        User user = findUserOrThrow(userId);
        MediaItem lastGenerated = user.lastGeneratedImdbId() == null
                ? null
                : mediaItemRepository.findById(user.lastGeneratedImdbId()).orElse(null);
        List<MediaItem> seenItems = mediaItemRepository.findByIds(user.seenMediaIds());
        return new UserDetails(user, lastGenerated, seenItems);
    }

    public void deleteUser(UserId userId) {
        findUserOrThrow(userId);
        userRepository.deleteById(userId);
    }

    public User markAsSeen(UserId userId, ImdbId imdbId) {
        User user = findUserOrThrow(userId);
        user.markAsSeen(imdbId);
        return userRepository.save(user);
    }

    public User unmarkAsSeen(UserId userId, ImdbId imdbId) {
        User user = findUserOrThrow(userId);
        user.unmarkAsSeen(imdbId);
        return userRepository.save(user);
    }

    private User findUserOrThrow(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));
    }

    public record UserDetails(User user, MediaItem lastGeneratedItem, List<MediaItem> seenItems) {
        public Optional<MediaItem> unseenLastGeneratedPrompt() {
            if (lastGeneratedItem != null && user.isLastGeneratedUnseen()) {
                return Optional.of(lastGeneratedItem);
            }
            return Optional.empty();
        }
    }
}
