package com.anurag.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Service for generating and validating JWT tokens.
 *
 * - Signing secret is read securely from environment variables / configuration
 * - Validates cryptographic key strength on application initialization (minimum 256 bits)
 * - Uses StandardCharsets.UTF_8 for consistent cross-platform byte encoding
 * - Token contains the user's email as subject
 * - Token expires after configured duration (default: 24 hours)
 */
@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration; // in milliseconds

    /**
     * Validate the signing secret on startup to ensure adequate cryptographic strength.
     * Prevents the application from running with unconfigured or weak keys.
     */
    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT signing secret is not configured. Please set the JWT_SECRET environment variable " +
                "or configure jwt.secret in application-local.properties."
            );
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT signing secret must be at least 256 bits (32 bytes) long for HMAC-SHA256. " +
                "Current secret length: " + jwtSecret.getBytes(StandardCharsets.UTF_8).length + " bytes."
            );
        }
    }

    /**
     * Build the signing key from the secret string.
     * HMAC-SHA256 requires at least 256-bit key.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for a given user.
     * The subject is the user's email (unique identifier).
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, jwtExpiration);
    }

    /**
     * Generate a JWT token with custom expiration (used for testing expired tokens).
     */
    public String generateToken(UserDetails userDetails, long expirationMs) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract the email (subject) from the JWT token.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Validate the token:
     * - Checks token is not expired
     * - Checks token subject matches the UserDetails username
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email != null && email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if the token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Parse and return all claims from the token.
     * Throws JwtException if token is invalid or expired.
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
