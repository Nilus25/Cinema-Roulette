package com.imdbselector.application.usecase.auth;

import com.imdbselector.application.port.out.TokenPort;
import com.imdbselector.domain.exception.UnauthorizedException;
import com.imdbselector.domain.valueobject.UserId;

import java.util.Objects;

public final class AuthenticateTokenUseCase {
    private final TokenPort tokenPort;

    public AuthenticateTokenUseCase(TokenPort tokenPort) {
        this.tokenPort = Objects.requireNonNull(tokenPort);
    }

    public UserId execute(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        return tokenPort.verifyToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
    }
}
