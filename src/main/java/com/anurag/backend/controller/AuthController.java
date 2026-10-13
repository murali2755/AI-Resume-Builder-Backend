package com.anurag.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.backend.dto.AuthResponse;
import com.anurag.backend.dto.LoginRequest;
import com.anurag.backend.dto.RegisterRequest;
import com.anurag.backend.dto.UserResponse;

import jakarta.validation.Valid;

/**
 * Controller for authentication endpoints.
 *
 * Public endpoints (no JWT required):
 *   POST /api/auth/register  — create new account
 *   POST /api/auth/login     — get JWT token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final com.anurag.backend.service.AuthService authService;

    public AuthController(com.anurag.backend.service.AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     *
     * Request body:
     * {
     *   "name": "John Doe",
     *   "email": "john@gmail.com",
     *   "password": "password123"
     * }
     *
     * Response: user info (no password), HTTP 201 Created
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Login and get JWT token.
     *
     * Request body:
     * {
     *   "email": "john@gmail.com",
     *   "password": "password123"
     * }
     *
     * Response:
     * {
     *   "message": "Login successful",
     *   "token": "JWT_TOKEN",
     *   "user": { "id": 1, "name": "John Doe", "email": "john@gmail.com" }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
