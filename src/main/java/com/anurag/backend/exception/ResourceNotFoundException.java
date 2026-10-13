package com.anurag.backend.exception;

/**
 * Thrown when a requested resource (user, resume) is not found.
 * Results in a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
