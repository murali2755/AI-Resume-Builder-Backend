package com.anurag.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.anurag.backend.dto.AuthResponse;
import com.anurag.backend.dto.LoginRequest;
import com.anurag.backend.dto.RegisterRequest;
import com.anurag.backend.dto.UserResponse;
import com.anurag.backend.exception.EmailAlreadyExistsException;
import com.anurag.backend.model.User;
import com.anurag.backend.repository.UserRepository;
import com.anurag.backend.security.CustomUserDetailsService;
import com.anurag.backend.security.JwtService;

/**
 * Service handling user registration and login.
 *
 * Registration flow:
 *   request → check email uniqueness → BCrypt hash → save to DB → return user info
 *
 * Login flow:
 *   request → authenticate with Spring Security → generate JWT → return token + user info
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Register a new user.
     *
     * - Checks if email is already taken (throws EmailAlreadyExistsException → HTTP 400)
     * - Hashes the password with BCrypt (NEVER stores plain text)
     * - Saves user to database
     * - Returns user info (no password)
     */
    public UserResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        // Create user entity
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Hash password with BCrypt before saving
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Save to database
        User savedUser = userRepository.save(user);

        // Return safe response (no password)
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    /**
     * Login with email and password.
     *
     * - Uses Spring Security's AuthenticationManager to verify credentials
     * - If credentials are wrong, AuthenticationManager throws BadCredentialsException
     * - If correct, generates JWT token
     * - Returns token + user info (no password)
     */
    public AuthResponse login(LoginRequest request) {
        // Let Spring Security authenticate — this uses BCrypt.matches() internally
        // If password is wrong, this throws BadCredentialsException → caught by GlobalExceptionHandler
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // If we reach here, authentication was successful
        // Load user from DB to get their info for the response
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Load UserDetails to generate JWT
        var userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);

        // Build response
        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail());
        return new AuthResponse("Login successful", token, userResponse);
    }
}