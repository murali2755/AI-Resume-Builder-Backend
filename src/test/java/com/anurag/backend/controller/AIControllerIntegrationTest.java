package com.anurag.backend.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.backend.dto.AiEnhanceRequest;
import com.anurag.backend.model.User;
import com.anurag.backend.repository.UserRepository;
import com.anurag.backend.security.JwtService;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class AIControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String uniqueEmail = "ai_test_" + UUID.randomUUID() + "@example.com";
        testUser = new User();
        testUser.setName("AI Tester");
        testUser.setEmail(uniqueEmail);
        testUser.setPassword(passwordEncoder.encode("Test@123456"));
        userRepository.save(testUser);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                testUser.getEmail(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        jwtToken = jwtService.generateToken(userDetails);
    }

    @AfterEach
    void tearDown() {
        if (testUser != null && testUser.getId() != null) {
            userRepository.deleteById(testUser.getId());
        }
    }

    @Test
    @DisplayName("GET /api/ai/status without auth token should return 401")
    void getStatus_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/ai/status with auth token should return 200 and provider info")
    void getStatus_WithToken_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/ai/status")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider", notNullValue()))
                .andExpect(jsonPath("$.model", notNullValue()))
                .andExpect(jsonPath("$.configured", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/ai/enhance without auth token should return 401")
    void enhance_WithoutToken_ReturnsUnauthorized() throws Exception {
        AiEnhanceRequest request = new AiEnhanceRequest("summary", "Sample professional summary text.");
        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/ai/enhance with blank content should return 400 Bad Request")
    void enhance_BlankContent_ReturnsBadRequest() throws Exception {
        AiEnhanceRequest request = new AiEnhanceRequest("summary", "   ");
        mockMvc.perform(post("/api/ai/enhance")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/ai/enhance with blank field should return 400 Bad Request")
    void enhance_BlankField_ReturnsBadRequest() throws Exception {
        AiEnhanceRequest request = new AiEnhanceRequest("", "Some valid text");
        mockMvc.perform(post("/api/ai/enhance")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
