package com.anurag.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.anurag.backend.security.CustomUserDetailsService;
import com.anurag.backend.security.JwtAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * Public endpoints (no JWT required):
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   /error
 *
 * Protected endpoints (JWT required):
 *   /api/resumes/**
 *   /api/ai/**
 *   any other unlisted endpoint (default-deny principle of least privilege)
 *
 * Uses:
 * - Stateless sessions (no server-side sessions)
 * - JWT filter before UsernamePasswordAuthenticationFilter
 * - BCrypt password encoder
 * - Configurable CORS from CorsConfig
 * - CSRF disabled (appropriate for stateless REST APIs)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService customUserDetailsService,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Main security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Use our CORS config (allows configured frontend origins)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Disable CSRF (appropriate for stateless JWT REST APIs)
            .csrf(csrf -> csrf.disable())

            // Define which endpoints are public vs protected
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Protected endpoints requiring JWT
                .requestMatchers("/api/resumes/**").authenticated()
                .requestMatchers("/api/ai/**").authenticated()
                // Default secure: any other requests require authentication
                .anyRequest().authenticated()
            )

            // Exception handling for unauthorized / unauthenticated requests
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                        "{\"status\":401,\"message\":\"Full authentication is required to access this resource\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(
                        "{\"status\":403,\"message\":\"Access denied\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}"
                    );
                })
            )

            // Stateless: no server-side HTTP sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Use our custom authentication provider (BCrypt + DB lookup)
            .authenticationProvider(authenticationProvider())

            // Add our JWT filter BEFORE Spring's default auth filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder bean.
     * Used in AuthService for hashing passwords and verifying them.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider that uses our database + BCrypt.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — needed for manual authentication in AuthService.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
