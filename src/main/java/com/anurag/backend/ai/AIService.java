package com.anurag.backend.ai;

import com.anurag.backend.dto.AiEnhanceRequest;
import com.anurag.backend.dto.AiEnhanceResponse;

/**
 * Service for genuine AI Resume Builder enhancements.
 * Supports OpenAI-compatible providers, Google Gemini, Groq, and local LLMs (e.g., Ollama).
 */
public interface AIService {

    /**
     * Enhance resume content for a specific section (summary, projects, experience, skills).
     *
     * @param request Request containing field name and raw user content
     * @return AiEnhanceResponse containing suggested enhancement and metadata
     */
    AiEnhanceResponse enhanceContent(AiEnhanceRequest request);

    /**
     * Enhance the professional summary section of a resume.
     *
     * @param summary The original summary text
     * @return Enhanced summary text
     */
    String enhanceSummary(String summary);

    /**
     * Improve the experience section of a resume.
     *
     * @param experience The original experience text
     * @return Improved experience text
     */
    String improveExperience(String experience);

    /**
     * Suggest relevant skills based on the role and existing skills.
     *
     * @param currentSkills Current skills listed on the resume
     * @param targetRole The job role the user is targeting
     * @return Suggested additional skills
     */
    String suggestSkills(String currentSkills, String targetRole);

    /**
     * Generate a professional project description.
     *
     * @param projectName Name of the project
     * @param technologies Technologies used
     * @return Generated project description
     */
    String generateProjectDescription(String projectName, String technologies);

    /**
     * Returns true when the AI provider is configured (i.e. AI_API_KEY is set).
     * Controllers use this for the /api/ai/status endpoint instead of injecting the raw key.
     */
    boolean isConfigured();

    /**
     * Returns the provider name (e.g. "openai", "gemini").
     */
    String getProvider();

    /**
     * Returns the model name (e.g. "gpt-4o-mini").
     */
    String getModel();
}
