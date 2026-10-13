package com.anurag.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.anurag.backend.dto.ResumeRequest;
import com.anurag.backend.dto.ResumeResponse;
import com.anurag.backend.exception.ResourceNotFoundException;
import com.anurag.backend.model.Resume;
import com.anurag.backend.model.User;
import com.anurag.backend.repository.ResumeRepository;
import com.anurag.backend.repository.UserRepository;

/**
 * Service for Resume CRUD operations.
 *
 * IMPORTANT SECURITY: The authenticatedEmail parameter comes from the JWT
 * (extracted from SecurityContext in the controller), NOT from the request body.
 * This ensures users can only access their own resumes.
 *
 * RESOURCE ENUMERATION MITIGATION (task E):
 * getResumeById, updateResume, and deleteResume return 404 for BOTH
 * non-existent AND cross-user resumes. This prevents an attacker from
 * probing which resume IDs exist by observing 403 vs 404 differences.
 */
@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new resume for the authenticated user.
     */
    public ResumeResponse createResume(ResumeRequest request, String authenticatedEmail) {
        User user = getUserByEmail(authenticatedEmail);

        Resume resume = new Resume();
        resume.setUser(user);
        mapRequestToResume(request, resume);

        Resume saved = resumeRepository.save(resume);
        return mapToResponse(saved);
    }

    /**
     * Get all resumes belonging to the authenticated user.
     */
    public List<ResumeResponse> getUserResumes(String authenticatedEmail) {
        User user = getUserByEmail(authenticatedEmail);

        return resumeRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific resume by ID — ONLY if it belongs to the authenticated user.
     *
     * Returns 404 for BOTH missing resumes AND resumes owned by another user.
     * This prevents resource enumeration: an attacker cannot tell from the response
     * whether a resume ID exists or simply belongs to someone else.
     */
    public ResumeResponse getResumeById(Long id, String authenticatedEmail) {
        User user = getUserByEmail(authenticatedEmail);

        // Single query: returns a result only when both ID and owner match
        Optional<Resume> optionalResume = resumeRepository.findByIdAndUser(id, user);
        if (optionalResume.isEmpty()) {
            throw new ResourceNotFoundException("Resume not found with id: " + id);
        }

        return mapToResponse(optionalResume.get());
    }

    /**
     * Update a resume — ONLY if it belongs to the authenticated user.
     *
     * PUT semantics: only non-null fields from the request are applied
     * (partial update / PATCH-like behaviour). Fields not included in the
     * request body retain their current value.
     *
     * Returns 404 for both missing and cross-user resumes (resource enumeration mitigation).
     */
    public ResumeResponse updateResume(Long id, ResumeRequest request, String authenticatedEmail) {
        User user = getUserByEmail(authenticatedEmail);

        Optional<Resume> optionalResume = resumeRepository.findByIdAndUser(id, user);
        if (optionalResume.isEmpty()) {
            throw new ResourceNotFoundException("Resume not found with id: " + id);
        }

        Resume resume = optionalResume.get();
        mapRequestToResume(request, resume);
        Resume updated = resumeRepository.save(resume);
        return mapToResponse(updated);
    }

    /**
     * Delete a resume — ONLY if it belongs to the authenticated user.
     *
     * Returns 404 for both missing and cross-user resumes (resource enumeration mitigation).
     */
    public void deleteResume(Long id, String authenticatedEmail) {
        User user = getUserByEmail(authenticatedEmail);

        Optional<Resume> optionalResume = resumeRepository.findByIdAndUser(id, user);
        if (optionalResume.isEmpty()) {
            throw new ResourceNotFoundException("Resume not found with id: " + id);
        }

        resumeRepository.delete(optionalResume.get());
    }

    // ---- Private helper methods ----

    /**
     * Load user by email from database.
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Copy fields from ResumeRequest to Resume entity.
     * Only updates non-null fields (supports partial updates — see updateResume Javadoc).
     */
    private void mapRequestToResume(ResumeRequest request, Resume resume) {
        if (request.getSummary() != null) resume.setSummary(request.getSummary());
        if (request.getCollege() != null) resume.setCollege(request.getCollege());
        if (request.getDegree() != null) resume.setDegree(request.getDegree());
        if (request.getBranch() != null) resume.setBranch(request.getBranch());
        if (request.getCgpa() != null) resume.setCgpa(request.getCgpa());
        if (request.getGraduationYear() != null) resume.setGraduationYear(request.getGraduationYear());
        if (request.getSkills() != null) resume.setSkills(request.getSkills());
        if (request.getProjects() != null) resume.setProjects(request.getProjects());
        if (request.getExperience() != null) resume.setExperience(request.getExperience());
    }

    /**
     * Convert Resume entity to ResumeResponse DTO.
     */
    private ResumeResponse mapToResponse(Resume resume) {
        ResumeResponse response = new ResumeResponse();
        response.setId(resume.getId());
        response.setUserId(resume.getUser().getId());
        response.setSummary(resume.getSummary());
        response.setCollege(resume.getCollege());
        response.setDegree(resume.getDegree());
        response.setBranch(resume.getBranch());
        response.setCgpa(resume.getCgpa());
        response.setGraduationYear(resume.getGraduationYear());
        response.setSkills(resume.getSkills());
        response.setProjects(resume.getProjects());
        response.setExperience(resume.getExperience());
        response.setCreatedAt(resume.getCreatedAt());
        return response;
    }
}
