package com.anurag.backend.dto;

/**
 * Response payload containing the suggested AI improvement.
 * Does not overwrite the saved resume automatically.
 */
public class AiEnhanceResponse {

    private String field;
    private String originalContent;
    private String enhancedContent;
    private String model;
    private String provider;

    public AiEnhanceResponse() {
    }

    public AiEnhanceResponse(String field, String originalContent, String enhancedContent, String model, String provider) {
        this.field = field;
        this.originalContent = originalContent;
        this.enhancedContent = enhancedContent;
        this.model = model;
        this.provider = provider;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getEnhancedContent() {
        return enhancedContent;
    }

    public void setEnhancedContent(String enhancedContent) {
        this.enhancedContent = enhancedContent;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
