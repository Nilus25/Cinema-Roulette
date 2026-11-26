package com.imdbselector.application.port.out;

import com.imdbselector.domain.valueobject.UserId;

import java.util.Optional;

public interface TokenPort {
    String issueToken(UserId userId, String username);

    Optional<UserId> verifyToken(String token);
}
