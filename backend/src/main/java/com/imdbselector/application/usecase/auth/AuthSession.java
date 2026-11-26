package com.imdbselector.application.usecase.auth;

import com.imdbselector.domain.model.User;

public record AuthSession(User user, String token) {
}
