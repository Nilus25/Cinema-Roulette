package com.imdbselector.domain.exception;

public final class InvalidQueryException extends DomainException {
    public InvalidQueryException(String message) {
        super(message);
    }
}
