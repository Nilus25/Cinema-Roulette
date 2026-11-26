package com.imdbselector.domain.exception;

public final class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(message);
    }
}
