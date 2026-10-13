package com.anurag.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anurag.backend.ai.AIService;
import com.anurag.backend.dto.AiEnhanceRequest;
import com.anurag.backend.dto.AiEnhanceResponse;

import jakarta.validation.Valid;

/**
 * Controller for authenticated AI enhancement endpoints.
 *
 * Base URL: /api/ai
 * ALL endpoints require JWT authentication.
 *
 * Design: this controller delegates entirely to AIService and does NOT inject
 * the raw AI_API_KEY — use aiService.isConfigured() instead (task F).
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * Enhance a resume section using genuine AI provider.
     * POST /api/ai/enhance
     *
     * @param request must include a valid field (summary|projects|experience|skills) and non-empty content
     */
    @PostMapping("/enhance")
    public ResponseEntity<AiEnhanceResponse> enhance(
            @Valid @RequestBody AiEnhanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AiEnhanceResponse response = aiService.enhanceContent(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if AI provider integration is configured on the backend.
     * GET /api/ai/status
     *
     * Never exposes the API key or secret values.
     * Uses aiService.isConfigured() — no raw key injected into this controller.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAiStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", aiService.isConfigured());
        status.put("provider", aiService.getProvider());
        status.put("model", aiService.getModel());
        return ResponseEntity.ok(status);
    }
}
