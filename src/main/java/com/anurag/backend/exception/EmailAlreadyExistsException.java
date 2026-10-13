package com.anurag.backend.exception;

/**
 * Thrown by AuthService when a registration email is already taken.
 * Returns HTTP 400 so the frontend can display a user-friendly message.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
