package com.imdbselector.domain.exception;

/**
 * Sealed base runtime exception for all expected business/domain rule violations.
 */
public abstract sealed class DomainException extends RuntimeException
        permits DuplicateResourceException,
                ResourceNotFoundException,
                NoMatchingMediaException,
                InvalidCsvException,
                UnauthorizedException,
                ForbiddenException {

    protected DomainException(String message) {
        super(message);
    }
}
