package com.imdbselector.domain.exception;

public final class NoMatchingMediaException extends DomainException {
    public NoMatchingMediaException(String message) {
        super(message);
    }
}
