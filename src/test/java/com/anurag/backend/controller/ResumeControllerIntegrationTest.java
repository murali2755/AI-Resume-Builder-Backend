package com.anurag.backend.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.anurag.backend.dto.ResumeRequest;
import com.anurag.backend.dto.ResumeResponse;
import com.anurag.backend.model.Resume;
import com.anurag.backend.model.User;
import com.anurag.backend.repository.ResumeRepository;
import com.anurag.backend.repository.UserRepository;
import com.anurag.backend.security.CustomUserDetailsService;
import com.anurag.backend.security.JwtService;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for ResumeController.
 * Verifies JWT authentication, CRUD operations, cross-user ownership isolation,
 * and error responses.
 */
@SpringBootTest
class ResumeControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        createdUserIds.clear();

        // Create User A
        String emailA = "resumetester_a_" + System.currentTimeMillis() + "@example.com";
        userA = new User();
        userA.setName("User Alpha");
        userA.setEmail(emailA);
        userA.setPassword(passwordEncoder.encode("password123"));
        userA = userRepository.save(userA);
        createdUserIds.add(userA.getId());

        var userDetailsA = customUserDetailsService.loadUserByUsername(emailA);
        tokenA = jwtService.generateToken(userDetailsA);

        // Create User B
        String emailB = "resumetester_b_" + (System.currentTimeMillis() + 1) + "@example.com";
        userB = new User();
        userB.setName("User Beta");
        userB.setEmail(emailB);
        userB.setPassword(passwordEncoder.encode("password123"));
        userB = userRepository.save(userB);
        createdUserIds.add(userB.getId());

        var userDetailsB = customUserDetailsService.loadUserByUsername(emailB);
        tokenB = jwtService.generateToken(userDetailsB);
    }

    @AfterEach
    void tearDown() {
        // Clean up resumes and users created by the test
        for (Integer userId : createdUserIds) {
            userRepository.findById(userId).ifPresent(u -> {
                List<Resume> userResumes = resumeRepository.findByUser(u);
                resumeRepository.deleteAll(userResumes);
                userRepository.delete(u);
            });
        }
    }

    @Test
    void accessWithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void accessWithInvalidToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer invalid.jwt.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void accessWithExpiredToken_ShouldReturn401() throws Exception {
        var userDetailsA = customUserDetailsService.loadUserByUsername(userA.getEmail());
        String expiredToken = jwtService.generateToken(userDetailsA, -1000L);

        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void accessWithTokenSignedWithDifferentKey_ShouldReturn401() throws Exception {
        javax.crypto.SecretKey differentKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "different_cryptographic_secret_key_minimum_32_bytes_long_123456".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        String untrustedToken = io.jsonwebtoken.Jwts.builder()
                .subject(userA.getEmail())
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(differentKey)
                .compact();

        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer " + untrustedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void createResume_ShouldReturn201_WhenAuthorized() throws Exception {
        ResumeRequest request = new ResumeRequest();
        request.setSummary("Passionate full stack developer");
        request.setCollege("Anurag University");
        request.setDegree("B.Tech");
        request.setBranch("CSE");
        request.setCgpa(8.8);
        request.setGraduationYear(2025);
        request.setSkills("Java, Spring Boot, React, MySQL");
        request.setProjects("AI Resume Builder");
        request.setExperience("Intern at Tech Corp");

        mockMvc.perform(post("/api/resumes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(userA.getId()))
                .andExpect(jsonPath("$.summary").value("Passionate full stack developer"))
                .andExpect(jsonPath("$.college").value("Anurag University"))
                .andExpect(jsonPath("$.skills").value("Java, Spring Boot, React, MySQL"));
    }

    @Test
    void getUserResumes_ShouldReturnOnlyCurrentUserResumes() throws Exception {
        // Create resume for User A
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("Alpha resume");
        resumeRepository.save(resumeA);

        // Create resume for User B
        Resume resumeB = new Resume();
        resumeB.setUser(userB);
        resumeB.setSummary("Beta resume");
        resumeRepository.save(resumeB);

        // User A requests resumes
        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].summary").value("Alpha resume"))
                .andExpect(jsonPath("$[0].userId").value(userA.getId()));
    }

    @Test
    void getResumeById_ShouldReturn200_WhenUserOwnsResume() throws Exception {
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("Alpha personal resume");
        resumeA = resumeRepository.save(resumeA);

        mockMvc.perform(get("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeA.getId()))
                .andExpect(jsonPath("$.summary").value("Alpha personal resume"));
    }

    @Test
    void updateResume_ShouldReturn200_WhenUserOwnsResume() throws Exception {
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("Initial summary");
        resumeA = resumeRepository.save(resumeA);

        ResumeRequest updateRequest = new ResumeRequest();
        updateRequest.setSummary("Updated summary");
        updateRequest.setSkills("Java, Spring, Docker");

        mockMvc.perform(put("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Updated summary"))
                .andExpect(jsonPath("$.skills").value("Java, Spring, Docker"));
    }

    @Test
    void deleteResume_ShouldReturn204_WhenUserOwnsResume() throws Exception {
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("To be deleted");
        resumeA = resumeRepository.save(resumeA);

        mockMvc.perform(delete("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    // ---- Cross-User Ownership Isolation Tests ----
    // NOTE: Cross-user access now returns 404 (not 403) to prevent resource enumeration.
    // An attacker cannot distinguish "this ID doesn't exist" from "this ID belongs to someone else".

    @Test
    void crossUserAccess_ShouldReturn404_WhenUserBAccessesUserAResume() throws Exception {
        // User A creates a resume
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("User A Private Resume");
        resumeA = resumeRepository.save(resumeA);

        // User B attempts to GET User A's resume — should get 404 (not 403)
        mockMvc.perform(get("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crossUserUpdate_ShouldReturn404_WhenUserBUpdatesUserAResume() throws Exception {
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("User A Resume");
        resumeA = resumeRepository.save(resumeA);

        ResumeRequest maliciousUpdate = new ResumeRequest();
        maliciousUpdate.setSummary("Hacked by User B");

        // User B attempts to PUT User A's resume — should get 404
        mockMvc.perform(put("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crossUserDelete_ShouldReturn404_WhenUserBDeletesUserAResume() throws Exception {
        Resume resumeA = new Resume();
        resumeA.setUser(userA);
        resumeA.setSummary("User A Resume");
        resumeA = resumeRepository.save(resumeA);

        // User B attempts to DELETE User A's resume — should get 404
        mockMvc.perform(delete("/api/resumes/" + resumeA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getResumeById_ShouldReturn404_WhenResumeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/resumes/999999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }
}
