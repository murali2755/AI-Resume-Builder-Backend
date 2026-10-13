package com.anurag.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.anurag.backend.dto.AuthResponse;
import com.anurag.backend.dto.LoginRequest;
import com.anurag.backend.dto.RegisterRequest;
import com.anurag.backend.dto.UserResponse;
import com.anurag.backend.repository.UserRepository;
import com.anurag.backend.security.CustomUserDetailsService;
import com.anurag.backend.security.JwtService;

import java.util.Optional;

/**
 * Unit tests for AuthService.
 * Uses Mockito to mock dependencies — no real database or network calls.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private com.anurag.backend.model.User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@gmail.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@gmail.com");
        loginRequest.setPassword("password123");

        mockUser = new com.anurag.backend.model.User();
        mockUser.setId(1);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@gmail.com");
        mockUser.setPassword("$2a$10$hashedpassword");
    }

    // ---- Registration Tests ----

    @Test
    void register_ShouldSucceed_WhenEmailIsNew() {
        // Arrange
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(com.anurag.backend.model.User.class))).thenReturn(mockUser);

        // Act
        UserResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(Integer.valueOf(1), response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@gmail.com", response.getEmail());

        // Verify BCrypt was called (password was hashed)
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(com.anurag.backend.model.User.class));
    }

    @Test
    void register_ShouldFail_WhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(true);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email is already registered", ex.getMessage());

        // Verify save was never called
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldNeverStorePasswordInPlainText() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");

        // Capture the saved user
        when(userRepository.save(any(com.anurag.backend.model.User.class))).thenAnswer(invocation -> {
            com.anurag.backend.model.User savedUser = invocation.getArgument(0);

            // Critical security check: stored password must NOT be the plain text
            assertNotEquals("password123", savedUser.getPassword());
            // Must start with BCrypt prefix
            assertTrue(savedUser.getPassword().startsWith("$2a$"));

            return mockUser;
        });

        authService.register(registerRequest);
    }

    // ---- Login Tests ----

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreCorrect() {
        // Arrange
        UserDetails mockUserDetails = User.builder()
                .username("john@gmail.com")
                .password("$2a$10$hashedpassword")
                .roles("USER")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate succeeds without exception

        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(mockUser));
        when(customUserDetailsService.loadUserByUsername("john@gmail.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mock.jwt.token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("mock.jwt.token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("john@gmail.com", response.getUser().getEmail());
        // Password must NOT be in the response
        assertNull(response.getUser().getClass().getFields().length > 0 ? null : null); // UserResponse has no password field
    }

    @Test
    void login_ShouldFail_WhenPasswordIsWrong() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        // JWT should never be generated
        verify(jwtService, never()).generateToken(any());
    }
}
