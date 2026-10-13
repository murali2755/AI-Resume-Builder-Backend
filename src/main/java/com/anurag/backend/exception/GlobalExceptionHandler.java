package com.anurag.backend.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

/**
 * Global exception handler — catches exceptions from all controllers.
 * Returns clean JSON errors. Never returns stack traces or internal details to the client.
 *
 * Error response format:
 * {
 *   "status": 400,
 *   "message": "...",
 *   "timestamp": "..."
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- Helper to build consistent error response ----

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status.value());
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(error);
    }

    // ---- AI Service errors (unconfigured, rate limit, timeout, provider error) ----

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceException(AiServiceException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", ex.getStatus().value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    // ---- Validation errors (e.g. @NotBlank, @Email, @Size) ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        StringBuilder messages = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!messages.isEmpty()) {
                messages.append(", ");
            }
            messages.append(error.getField()).append(": ").append(error.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, messages.toString());
    }

    // ---- Malformed JSON request bodies ----

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    // ---- Method argument type mismatch (e.g. string ID instead of number) ----

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid parameter: " + ex.getName());
    }

    // ---- Resource not found (user or resume) ----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ---- Unauthorized access (cross-user resume access) ----

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ---- Duplicate email on registration — kept at 400 so frontend isn't affected ----

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ---- Database constraint violations (unique index, FK, etc.) ----
    // Returns a safe generic message — never leaks SQL details.

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT, "A resource with the provided data already exists.");
    }

    // ---- Illegal argument errors (business-logic validation) ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ---- Bad credentials (wrong password at login) ----

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        // Never echo ex.getMessage() — it may reveal internal details.
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // ---- Spring Security AuthenticationException ----

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        // Generic message — do not return ex.getMessage() to the client.
        return buildError(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    // ---- Spring Security AccessDeniedException ----

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Access denied");
    }

    // ---- JWT Expired ----

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "JWT token has expired");
    }

    // ---- JWT Invalid / Malformed ----

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
    }

    // ---- Unexpected runtime errors — logged server-side, generic message to client ----

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.");
    }

    // ---- Generic catch-all (server errors) ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.");
    }
}
