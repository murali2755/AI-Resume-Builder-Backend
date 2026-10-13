package com.anurag.backend.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.anurag.backend.dto.AiEnhanceRequest;
import com.anurag.backend.dto.AiEnhanceResponse;
import com.anurag.backend.exception.AiServiceException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Genuine implementation of AIService connecting to OpenAI-compatible AI providers,
 * including OpenAI, Google Gemini (via OpenAI-compatible endpoint), Groq, and local Ollama.
 *
 * Configured via:
 *   AI_PROVIDER (default: openai)
 *   AI_BASE_URL (default: https://api.openai.com/v1)
 *   AI_API_KEY (backend-only secret — never exposed to the client)
 *   AI_MODEL (default: gpt-4o-mini)
 *   AI_TIMEOUT_SECONDS (default: 30)
 */
@Service
public class AIServiceImpl implements AIService {

    /** Allowed section names for the 'field' parameter. */
    private static final Set<String> ALLOWED_FIELDS = Set.of("summary", "projects", "experience", "skills");

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AIServiceImpl(
            @Value("${ai.provider:openai}") String provider,
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gpt-4o-mini}") String model,
            @Value("${ai.timeout-seconds:30}") int timeoutSeconds,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this(provider, baseUrl, apiKey, model, timeoutSeconds,
             HttpClient.newBuilder()
                     // Connect timeout separate from per-request timeout (task F)
                     .connectTimeout(Duration.ofSeconds(10))
                     .build(),
             objectMapper != null ? objectMapper : new ObjectMapper());
    }

    public AIServiceImpl(
            String provider,
            String baseUrl,
            String apiKey,
            String model,
            int timeoutSeconds,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.provider = provider != null ? provider.trim() : "openai";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://api.openai.com/v1";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null ? model.trim() : "gpt-4o-mini";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    // ---- AIService interface: metadata ----

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getModel() {
        return model;
    }

    // ---- AIService interface: enhancement operations ----

    @Override
    public AiEnhanceResponse enhanceContent(AiEnhanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String field = request.getField() != null ? request.getField().trim().toLowerCase() : "";
        if (field.isEmpty()) {
            throw new IllegalArgumentException("Field name is required");
        }

        // Validate field against allowed set (task F)
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                "Invalid field '" + field + "'. Allowed values: " + ALLOWED_FIELDS);
        }

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }

        if (content.length() > 4000) {
            throw new IllegalArgumentException("Content exceeds maximum allowed size of 4000 characters");
        }

        // Verify API key configuration
        if (apiKey.isEmpty()) {
            throw new AiServiceException(
                "AI enhancement service is not configured. Please set the AI_API_KEY environment variable on the backend.",
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_NOT_CONFIGURED"
            );
        }

        String systemPrompt = buildSystemPrompt(field);
        String userPrompt = buildUserPrompt(field, content, request.getTargetRole(), request.getTechnologies());

        String rawAiOutput = callAiProvider(systemPrompt, userPrompt);
        String cleanedOutput = cleanOutput(rawAiOutput);

        return new AiEnhanceResponse(field, request.getContent(), cleanedOutput, model, provider);
    }

    @Override
    public String enhanceSummary(String summary) {
        AiEnhanceResponse response = enhanceContent(new AiEnhanceRequest("summary", summary));
        return response.getEnhancedContent();
    }

    @Override
    public String improveExperience(String experience) {
        AiEnhanceResponse response = enhanceContent(new AiEnhanceRequest("experience", experience));
        return response.getEnhancedContent();
    }

    @Override
    public String suggestSkills(String currentSkills, String targetRole) {
        AiEnhanceResponse response = enhanceContent(new AiEnhanceRequest("skills", currentSkills, targetRole, null));
        return response.getEnhancedContent();
    }

    @Override
    public String generateProjectDescription(String projectName, String technologies) {
        AiEnhanceResponse response = enhanceContent(new AiEnhanceRequest("projects", projectName, null, technologies));
        return response.getEnhancedContent();
    }

    // ---- Internal AI calling logic ----

    private String callAiProvider(String systemPrompt, String userPrompt) {
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl.replaceAll("/+$", "") + "/chat/completions";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 1024);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        payload.put("messages", messages);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new AiServiceException("Failed to serialize AI request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "SERIALIZATION_ERROR");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    JsonNode messageNode = choices.get(0).path("message");
                    String content = messageNode.path("content").asText();
                    if (content != null && !content.trim().isEmpty()) {
                        return content.trim();
                    }
                }
                throw new AiServiceException("AI provider returned an empty response.", HttpStatus.BAD_GATEWAY, "EMPTY_RESPONSE");
            } else if (status == 401 || status == 403) {
                throw new AiServiceException("AI provider authentication failed. Please check the AI_API_KEY configuration.", HttpStatus.BAD_GATEWAY, "AUTHENTICATION_FAILED");
            } else if (status == 429) {
                throw new AiServiceException("AI provider rate limit reached. Please wait a moment before trying again.", HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
            } else {
                throw new AiServiceException("AI provider returned error status: " + status, HttpStatus.BAD_GATEWAY, "PROVIDER_ERROR");
            }
        } catch (HttpTimeoutException e) {
            throw new AiServiceException("AI provider request timed out after " + timeoutSeconds + " seconds.", HttpStatus.GATEWAY_TIMEOUT, "TIMEOUT");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiServiceException("Failed to communicate with AI provider: " + e.getMessage(), HttpStatus.BAD_GATEWAY, "COMMUNICATION_ERROR");
        }
    }

    private String buildSystemPrompt(String field) {
        return "You are an expert career consultant and ATS resume optimization assistant. "
                + "Your task is to enhance the user's resume content for high impact, professional clarity, and recruiter ATS screening.\n"
                + "CRITICAL RULES:\n"
                + "1. Strictly preserve the user's authentic facts, accomplishments, qualifications, technologies, and career trajectory.\n"
                + "2. DO NOT invent false qualifications, degrees, companies, jobs, unmentioned technologies, or fabricated metrics.\n"
                + "3. Output ONLY the improved text. Do NOT include markdown code fences (```), meta-commentary, introductory remarks ('Here is the enhanced version'), or conversational explanations.";
    }

    private String buildUserPrompt(String field, String content, String targetRole, String technologies) {
        StringBuilder sb = new StringBuilder();
        sb.append("Section to enhance: ").append(field.toUpperCase()).append("\n");

        if (targetRole != null && !targetRole.trim().isEmpty()) {
            sb.append("Target Role / Focus: ").append(targetRole.trim()).append("\n");
        }
        if (technologies != null && !technologies.trim().isEmpty()) {
            sb.append("Associated Technologies: ").append(technologies.trim()).append("\n");
        }

        sb.append("\nOriginal Content:\n").append(content).append("\n\n");

        switch (field) {
            case "summary" -> sb.append("Enhance this professional summary into 2-3 impactful sentences highlighting technical strengths, problem solving, and career focus. Use strong verbs.");
            case "experience" -> sb.append("Enhance these experience descriptions into clean bullet points starting with '• '. Begin each point with strong past-tense action verbs. Preserve exact metrics if present.");
            case "projects" -> sb.append("Enhance these project details into structured points starting with '• Project Name: Description'. Emphasize architecture, implementation, and technologies used.");
            case "skills" -> sb.append("Standardize and organize these technical skills into a clean, comma-separated list of relevant industry-standard technologies and keywords.");
            default -> sb.append("Enhance this content for professional ATS readability while strictly preserving meaning.");
        }

        return sb.toString();
    }

    private String cleanOutput(String output) {
        if (output == null) return "";
        String text = output.trim();
        // Remove surrounding markdown code fences if model wrapped response
        if (text.startsWith("```") && text.endsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        // Remove surrounding quotes if model added them
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }
}
