package com.anurag.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for AI resume content enhancement.
 * Supported fields: "summary", "projects", "experience", "skills".
 */
public class AiEnhanceRequest {

    @NotBlank(message = "Field name is required (e.g. summary, projects, experience, skills)")
    private String field;

    @NotBlank(message = "Content to enhance cannot be empty")
    @Size(max = 4000, message = "Content cannot exceed 4000 characters")
    private String content;

    @Size(max = 500, message = "Target role context cannot exceed 500 characters")
    private String targetRole;

    @Size(max = 500, message = "Technologies context cannot exceed 500 characters")
    private String technologies;

    public AiEnhanceRequest() {
    }

    public AiEnhanceRequest(String field, String content) {
        this.field = field;
        this.content = content;
    }

    public AiEnhanceRequest(String field, String content, String targetRole, String technologies) {
        this.field = field;
        this.content = content;
        this.targetRole = targetRole;
        this.technologies = technologies;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }
}
