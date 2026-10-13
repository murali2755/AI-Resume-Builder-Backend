package com.anurag.backend.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter — runs once per HTTP request.
 *
 * Flow:
 * 1. Extract JWT from "Authorization: Bearer <token>" header
 * 2. Validate token signature and expiration
 * 3. If valid, load user from DB and set authentication in SecurityContext
 * 4. Spring Security then allows the request to proceed to the controller
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Get the Authorization header
        String authHeader = request.getHeader("Authorization");

        // If no Bearer token present, skip this filter (request will fail security if protected)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // Step 3: Extract email from token
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            // Invalid, tampered, or expired token — continue without setting auth (rejected by security)
            filterChain.doFilter(request, response);
            return;
        }

        // Step 4: If email extracted and no authentication is set yet
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // Step 5: Validate the token
                if (jwtService.isTokenValid(token, userDetails)) {
                    // Step 6: Create authentication token and set in security context
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // User not found in database or lookup failed — do not authenticate
            }
        }

        // Step 7: Continue to the next filter/controller
        filterChain.doFilter(request, response);
    }
}
