package com.imdbselector.application.usecase;

import com.imdbselector.application.port.out.PasswordHasherPort;
import com.imdbselector.application.port.out.TokenPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.exception.UnauthorizedException;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.UserId;

import java.util.Objects;

public final class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokenPort;

    public AuthService(UserRepository userRepository, PasswordHasherPort passwordHasher, TokenPort tokenPort) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenPort = Objects.requireNonNull(tokenPort);
    }

    public AuthSession register(String username, String rawPassword) {
        String normalized = normalizeUsername(username);
        validatePassword(rawPassword);

        userRepository.findByName(normalized).ifPresent(existing -> {
            throw new DuplicateResourceException("Username already taken: " + normalized);
        });

        String hash = passwordHasher.hash(rawPassword);
        User created = userRepository.save(User.register(normalized, hash));
        String token = tokenPort.issueToken(created.id(), created.name());
        return new AuthSession(created, token);
    }

    public AuthSession login(String username, String rawPassword) {
        String normalized = normalizeUsername(username);
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new UnauthorizedException("Invalid username or password");
        }

        User user = userRepository.findByName(normalized)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (user.passwordHash() == null || !passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = tokenPort.issueToken(user.id(), user.name());
        return new AuthSession(user, token);
    }

    public UserId authenticateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        return tokenPort.verifyToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 2 || trimmed.length() > 64) {
            throw new IllegalArgumentException("Username must be between 2 and 64 characters");
        }
        return trimmed;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    public record AuthSession(User user, String token) {
    }
}
