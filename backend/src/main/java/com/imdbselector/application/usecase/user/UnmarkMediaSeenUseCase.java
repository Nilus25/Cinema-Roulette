package com.imdbselector.application.usecase.user;

import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;

import java.util.Objects;

public final class UnmarkMediaSeenUseCase {
    private final UserRepository userRepository;

    public UnmarkMediaSeenUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public User execute(UserId userId, ImdbId imdbId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));
        user.unmarkAsSeen(imdbId);
        return userRepository.save(user);
    }
}
