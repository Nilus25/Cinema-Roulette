package com.imdbselector.domain.exception;

/**
 * Base runtime exception for domain rule violations.
 */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
