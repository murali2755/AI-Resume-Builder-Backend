package com.anurag.backend.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.backend.dto.LoginRequest;
import com.anurag.backend.dto.RegisterRequest;
import com.anurag.backend.model.User;
import com.anurag.backend.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for AuthController.
 * Tests registration, validation, login, and error responses through MockMvc.
 */
@SpringBootTest
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    private final List<String> testEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        testEmails.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up only the specific test users created during tests
        for (String email : testEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
    }

    @Test
    void register_ShouldReturn201AndUserInfo_WhenRequestIsValid() throws Exception {
        String email = "integ_reg_" + System.currentTimeMillis() + "@example.com";
        testEmails.add(email);

        RegisterRequest request = new RegisterRequest();
        request.setName("Integration User");
        request.setEmail(email);
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Integration User"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_ShouldReturn400_WhenEmailAlreadyExists() throws Exception {
        String email = "integ_dup_" + System.currentTimeMillis() + "@example.com";
        testEmails.add(email);

        // Register first time
        RegisterRequest request = new RegisterRequest();
        request.setName("First User");
        request.setEmail(email);
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Register second time with same email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void register_ShouldReturn400_WhenValidationFails() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName(""); // blank name
        request.setEmail("not-an-email"); // invalid email
        request.setPassword("123"); // short password

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void login_ShouldReturn200AndToken_WhenCredentialsAreValid() throws Exception {
        String email = "integ_login_" + System.currentTimeMillis() + "@example.com";
        testEmails.add(email);

        // First register
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setName("Login Tester");
        regRequest.setEmail(email);
        regRequest.setPassword("secretPass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // Now login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("secretPass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.name").value("Login Tester"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void login_ShouldReturn401_WhenPasswordIsIncorrect() throws Exception {
        String email = "integ_wrongpass_" + System.currentTimeMillis() + "@example.com";
        testEmails.add(email);

        // Register user
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setName("Wrong Pass Tester");
        regRequest.setEmail(email);
        regRequest.setPassword("correctPassword");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    void login_ShouldReturn400_WhenRequiredFieldsAreMissing() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("");
        loginRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
