package com.imdbselector.application.usecase.auth;

import com.imdbselector.application.port.out.PasswordHasherPort;
import com.imdbselector.application.port.out.TokenPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.UnauthorizedException;
import com.imdbselector.domain.model.User;

import java.util.Objects;

public final class LoginUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokenPort;

    public LoginUserUseCase(UserRepository userRepository, PasswordHasherPort passwordHasher, TokenPort tokenPort) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenPort = Objects.requireNonNull(tokenPort);
    }

    public AuthSession execute(String username, String rawPassword) {
        String normalized = CredentialsValidator.normalizeUsername(username);
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
}
