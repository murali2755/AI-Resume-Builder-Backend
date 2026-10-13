package com.anurag.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.backend.dto.ResumeRequest;
import com.anurag.backend.dto.ResumeResponse;
import com.anurag.backend.service.ResumeService;

import jakarta.validation.Valid;

/**
 * Controller for Resume CRUD endpoints.
 *
 * ALL endpoints require JWT authentication (configured in SecurityConfig).
 *
 * SECURITY: The authenticated user is extracted from the JWT via
 * @AuthenticationPrincipal — we NEVER trust a userId from the request body.
 * This prevents users from accessing each other's resumes.
 *
 * Base URL: /api/resumes
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /**
     * Create a new resume for the authenticated user.
     * POST /api/resumes
     */
    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(
            @Valid @RequestBody ResumeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername(); // email from JWT
        ResumeResponse response = resumeService.createResume(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all resumes for the authenticated user.
     * GET /api/resumes
     */
    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getUserResumes(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        List<ResumeResponse> resumes = resumeService.getUserResumes(email);
        return ResponseEntity.ok(resumes);
    }

    /**
     * Get a specific resume by ID.
     * Returns 403 if resume belongs to another user.
     * GET /api/resumes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        ResumeResponse response = resumeService.getResumeById(id, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a resume by ID.
     * Returns 403 if resume belongs to another user.
     * PUT /api/resumes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResume(
            @PathVariable Long id,
            @Valid @RequestBody ResumeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        ResumeResponse response = resumeService.updateResume(id, request, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a resume by ID.
     * Returns 403 if resume belongs to another user.
     * DELETE /api/resumes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        resumeService.deleteResume(id, email);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }
}
