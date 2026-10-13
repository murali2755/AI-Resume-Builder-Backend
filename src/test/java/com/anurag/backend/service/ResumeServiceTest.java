package com.anurag.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anurag.backend.dto.ResumeRequest;
import com.anurag.backend.dto.ResumeResponse;
import com.anurag.backend.exception.ResourceNotFoundException;
import com.anurag.backend.model.Resume;
import com.anurag.backend.model.User;
import com.anurag.backend.repository.ResumeRepository;
import com.anurag.backend.repository.UserRepository;

/**
 * Unit tests for ResumeService.
 * Tests CRUD operations and ownership enforcement.
 *
 * NOTE: Cross-user access now returns ResourceNotFoundException (404) rather than
 * UnauthorizedException (403) to prevent resource enumeration (task E).
 */
@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResumeService resumeService;

    private User userA;
    private User userB;
    private Resume resumeA;
    private ResumeRequest resumeRequest;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1);
        userA.setName("User A");
        userA.setEmail("usera@gmail.com");

        userB = new User();
        userB.setId(2);
        userB.setName("User B");
        userB.setEmail("userb@gmail.com");

        resumeA = new Resume();
        resumeA.setId(10L);
        resumeA.setUser(userA);
        resumeA.setSummary("Java developer with 2 years experience");
        resumeA.setCollege("Anurag University");
        resumeA.setSkills("Java, Spring Boot, MySQL");

        resumeRequest = new ResumeRequest();
        resumeRequest.setSummary("Updated summary");
        resumeRequest.setSkills("Java, Spring Boot, React");
    }

    // ---- Create Tests ----

    @Test
    void createResume_ShouldSucceed_ForAuthenticatedUser() {
        when(userRepository.findByEmail("usera@gmail.com")).thenReturn(Optional.of(userA));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resumeA);

        ResumeResponse response = resumeService.createResume(resumeRequest, "usera@gmail.com");

        assertNotNull(response);
        verify(resumeRepository).save(any(Resume.class));
    }

    // ---- Read Tests ----

    @Test
    void getUserResumes_ShouldReturnOnlyUserResumes() {
        when(userRepository.findByEmail("usera@gmail.com")).thenReturn(Optional.of(userA));
        when(resumeRepository.findByUser(userA)).thenReturn(List.of(resumeA));

        List<ResumeResponse> resumes = resumeService.getUserResumes("usera@gmail.com");

        assertEquals(1, resumes.size());
        assertEquals(10L, resumes.get(0).getId());
    }

    @Test
    void getResumeById_ShouldSucceed_WhenUserOwnsResume() {
        when(userRepository.findByEmail("usera@gmail.com")).thenReturn(Optional.of(userA));
        when(resumeRepository.findByIdAndUser(10L, userA)).thenReturn(Optional.of(resumeA));

        ResumeResponse response = resumeService.getResumeById(10L, "usera@gmail.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
    }

    /**
     * Cross-user access now returns ResourceNotFoundException (404), not UnauthorizedException (403).
     * This mitigates resource enumeration: an attacker cannot tell whether an ID exists by the status code.
     */
    @Test
    void getResumeById_ShouldThrowNotFound_WhenUserDoesNotOwnResume() {
        // User B tries to access User A's resume (resume ID 10)
        when(userRepository.findByEmail("userb@gmail.com")).thenReturn(Optional.of(userB));
        // findByIdAndUser returns empty for User B regardless of whether the resume exists
        when(resumeRepository.findByIdAndUser(10L, userB)).thenReturn(Optional.empty());

        // Previously threw UnauthorizedException (403) — now throws ResourceNotFoundException (404)
        assertThrows(ResourceNotFoundException.class,
                () -> resumeService.getResumeById(10L, "userb@gmail.com"));
    }

    @Test
    void getResumeById_ShouldThrowNotFound_WhenResumeDoesNotExist() {
        when(userRepository.findByEmail("usera@gmail.com")).thenReturn(Optional.of(userA));
        when(resumeRepository.findByIdAndUser(999L, userA)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> resumeService.getResumeById(999L, "usera@gmail.com"));
    }

    // ---- Update Tests ----

    @Test
    void updateResume_ShouldSucceed_WhenUserOwnsResume() {
        when(userRepository.findByEmail("usera@gmail.com")).thenReturn(Optional.of(userA));
        when(resumeRepository.findByIdAndUser(10L, userA)).thenReturn(Optional.of(resumeA));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resumeA);

        ResumeResponse response = resumeService.updateResume(10L, resumeRequest, "usera@gmail.com");

        assertNotNull(response);
        verify(resumeRepository).save(resumeA);
    }

    /**
     * Cross-user update returns ResourceNotFoundException (404) — resource enumeration mitigation.
     */
    @Test
    void updateResume_ShouldThrowNotFound_WhenUserDoesNotOwnResume() {
        when(userRepository.findByEmail("userb@gmail.com")).thenReturn(Optional.of(userB));
        when(resumeRepository.findByIdAndUser(10L, userB)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> resumeService.updateResume(10L, resumeRequest, "userb@gmail.com"));
    }

    // ---- Delete Tests ----

    @Test
    void deleteResume_ShouldSucceed_WhenUserOwnsResume() {
        when(userRepository.findByEmail("usera@gmail.com")).thenReturn(Optional.of(userA));
        when(resumeRepository.findByIdAndUser(10L, userA)).thenReturn(Optional.of(resumeA));

        resumeService.deleteResume(10L, "usera@gmail.com");

        verify(resumeRepository).delete(resumeA);
    }

    /**
     * Cross-user delete returns ResourceNotFoundException (404) — resource enumeration mitigation.
     */
    @Test
    void deleteResume_ShouldThrowNotFound_WhenUserDoesNotOwnResume() {
        when(userRepository.findByEmail("userb@gmail.com")).thenReturn(Optional.of(userB));
        when(resumeRepository.findByIdAndUser(10L, userB)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> resumeService.deleteResume(10L, "userb@gmail.com"));

        // Verify nothing was deleted
        verify(resumeRepository, never()).delete(any(Resume.class));
        verify(resumeRepository, never()).deleteById(anyLong());
    }
}
