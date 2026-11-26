package com.imdbselector.application.usecase.auth;

import com.imdbselector.application.port.out.PasswordHasherPort;
import com.imdbselector.application.port.out.TokenPort;
import com.imdbselector.application.port.out.TransactionPort;
import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.exception.DuplicateResourceException;
import com.imdbselector.domain.model.User;

import java.util.Objects;

public final class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokenPort;
    private final TransactionPort transactionPort;

    public RegisterUserUseCase(
            UserRepository userRepository,
            PasswordHasherPort passwordHasher,
            TokenPort tokenPort,
            TransactionPort transactionPort
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenPort = Objects.requireNonNull(tokenPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
    }

    public AuthSession execute(String username, String rawPassword) {
        String normalized = CredentialsValidator.normalizeUsername(username);
        CredentialsValidator.validatePassword(rawPassword);
        String hash = passwordHasher.hash(rawPassword);

        User created = transactionPort.inTransaction(() -> {
            userRepository.findByName(normalized).ifPresent(existing -> {
                throw new DuplicateResourceException("Username already taken: " + normalized);
            });
            return userRepository.save(User.register(normalized, hash));
        });

        String token = tokenPort.issueToken(created.id(), created.name());
        return new AuthSession(created, token);
    }
}
