package com.imdbselector.application.usecase.auth;

import com.imdbselector.application.port.out.PasswordHasherPort;
import com.imdbselector.application.port.out.TokenPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.ResourceNotFoundException;
import com.imdbselector.domain.exception.UnauthorizedException;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.UserId;

import java.util.Objects;

/**
 * Verifies the caller's current password, updates their BCrypt hash, and issues a fresh session token.
 */
public final class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokenPort;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            PasswordHasherPort passwordHasher,
            TokenPort tokenPort
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenPort = Objects.requireNonNull(tokenPort);
    }

    public AuthSession execute(UserId userId, String currentPassword, String newPassword) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId.value()));

        if (user.passwordHash() == null || !passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        CredentialsValidator.validatePassword(newPassword);
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        String updatedHash = passwordHasher.hash(newPassword);
        user.changePasswordHash(updatedHash);
        User saved = userRepository.save(user);
        String token = tokenPort.issueToken(saved.id(), saved.name());
        return new AuthSession(saved, token);
    }
}
