package com.anurag.backend.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.anurag.backend.repository.UserRepository;

/**
 * Loads user details from the database for Spring Security.
 * Spring Security uses this during JWT validation to get user info.
 *
 * The "username" in Spring Security context = email in our app.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Called by Spring Security with the email (our "username").
     * Loads the user from the database and wraps in Spring's UserDetails.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<com.anurag.backend.model.User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        com.anurag.backend.model.User appUser = userOptional.get();

        // Build Spring Security's UserDetails using the user's email and hashed password
        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPassword())
                .roles("USER")
                .build();
    }
}
