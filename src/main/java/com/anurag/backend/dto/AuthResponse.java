package com.anurag.backend.dto;

/**
 * DTO returned after successful login.
 * Contains JWT token + user info (no password).
 */
public class AuthResponse {

    private String message;
    private String token;
    private UserResponse user;

    // Default constructor
    public AuthResponse() {
    }

    public AuthResponse(String message, String token, UserResponse user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // Getters and Setters

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
