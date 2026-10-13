package com.anurag.backend.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

/**
 * Simple in-memory rate-limiting filter (task G).
 *
 * Uses a sliding-window bucket per key (IP or authenticated email).
 * Buckets are reset every windowSeconds.
 *
 * Limits (configurable via properties):
 *   POST /api/auth/login    — rate.limit.login.max (default 10) per rate.limit.window-seconds (default 60)
 *   POST /api/ai/enhance    — rate.limit.ai.max (default 20) per rate.limit.window-seconds (default 60)
 *
 * Returns HTTP 429 in the same JSON error format as GlobalExceptionHandler.
 *
 * NOTE: This is a single-node, in-memory solution suitable for development and
 * single-instance deployments. For multi-instance/distributed deployments, replace
 * with Redis-backed rate limiting (e.g. via Spring Data Redis + Bucket4j).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate.limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${rate.limit.login.max:10}")
    private int loginMax;

    @Value("${rate.limit.ai.max:20}")
    private int aiMax;

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String AI_ENHANCE_PATH = "/api/ai/enhance";

    private final ObjectMapper objectMapper;

    /**
     * Bucket: holds a counter and the timestamp when the window started.
     */
    private static class Bucket {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    // Separate maps for login (keyed by IP) and AI enhance (keyed by authenticated user)
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> aiBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getServletPath();

        if ("POST".equalsIgnoreCase(method) && LOGIN_PATH.equals(path)) {
            String key = getClientIp(request);
            if (!allowRequest(loginBuckets, key, loginMax)) {
                sendRateLimitError(response, "Too many login attempts. Please wait before trying again.");
                return;
            }
        } else if ("POST".equalsIgnoreCase(method) && AI_ENHANCE_PATH.equals(path)) {
            String key = getAuthenticatedUser();
            if (key != null && !allowRequest(aiBuckets, key, aiMax)) {
                sendRateLimitError(response, "AI enhancement rate limit exceeded. Please wait before trying again.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if the request is within the rate limit.
     * Returns true if allowed, false if the limit has been exceeded.
     */
    private boolean allowRequest(Map<String, Bucket> buckets, String key, int maxRequests) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());

        long now = System.currentTimeMillis();
        long windowMs = (long) windowSeconds * 1000;

        // Reset window if it has expired
        if (now - bucket.windowStart >= windowMs) {
            synchronized (bucket) {
                if (now - bucket.windowStart >= windowMs) {
                    bucket.count.set(0);
                    bucket.windowStart = now;
                }
            }
        }

        return bucket.count.incrementAndGet() <= maxRequests;
    }

    /**
     * Get the real client IP, respecting common proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Get the currently authenticated user's name (email), or null if not yet authenticated.
     */
    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    /**
     * Write a 429 Too Many Requests response in the same JSON format as GlobalExceptionHandler.
     */
    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
            "status", 429,
            "message", message,
            "timestamp", LocalDateTime.now().toString()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
