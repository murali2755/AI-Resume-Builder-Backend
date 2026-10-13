package com.anurag.backend.exception;

/**
 * Thrown when a user tries to access a resource they don't own.
 * Results in a 403 Forbidden response.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
