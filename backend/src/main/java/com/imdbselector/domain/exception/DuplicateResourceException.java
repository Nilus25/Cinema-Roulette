package com.imdbselector.domain.exception;

public final class DuplicateResourceException extends DomainException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
