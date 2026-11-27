package com.imdbselector.application.usecase.user;

import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.QueryFlavor;
import com.imdbselector.domain.valueobject.UserId;

import java.util.Objects;

public final class UpdateQueryFlavorUseCase {
    private final UserRepository userRepository;

    public UpdateQueryFlavorUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public User execute(UserId userId, QueryFlavor flavor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));
        user.changeQueryFlavor(flavor);
        return userRepository.save(user);
    }
}
