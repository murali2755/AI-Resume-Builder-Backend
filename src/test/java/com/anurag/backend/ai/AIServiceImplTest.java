package com.anurag.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.anurag.backend.dto.AiEnhanceRequest;
import com.anurag.backend.dto.AiEnhanceResponse;
import com.anurag.backend.exception.AiServiceException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private HttpResponse mockHttpResponse;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when request is null")
    void enhanceContent_NullRequest_ThrowsException() {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "test-key", "gpt-4o-mini", 30, mockHttpClient, objectMapper);
        assertThrows(IllegalArgumentException.class, () -> service.enhanceContent(null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when field is empty")
    void enhanceContent_EmptyField_ThrowsException() {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "test-key", "gpt-4o-mini", 30, mockHttpClient, objectMapper);
        AiEnhanceRequest request = new AiEnhanceRequest("", "Content");
        assertThrows(IllegalArgumentException.class, () -> service.enhanceContent(request));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when content is empty")
    void enhanceContent_EmptyContent_ThrowsException() {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "test-key", "gpt-4o-mini", 30, mockHttpClient, objectMapper);
        AiEnhanceRequest request = new AiEnhanceRequest("summary", "   ");
        assertThrows(IllegalArgumentException.class, () -> service.enhanceContent(request));
    }

    @Test
    @DisplayName("Should throw AiServiceException (503) when API key is missing")
    void enhanceContent_MissingApiKey_ThrowsServiceUnavailable() {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "", "gpt-4o-mini", 30, mockHttpClient, objectMapper);
        AiEnhanceRequest request = new AiEnhanceRequest("summary", "Experienced React developer with 3 years building web apps.");

        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.enhanceContent(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("AI_NOT_CONFIGURED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("AI_API_KEY"));
    }

    @Test
    @DisplayName("Should successfully enhance content when provider returns 200 OK")
    @SuppressWarnings("unchecked")
    void enhanceContent_Success_ReturnsEnhancedResponse() throws Exception {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "sk-valid-key", "gpt-4o-mini", 30, mockHttpClient, objectMapper);

        String providerJsonResponse = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Engineered high-performance React frontends reducing render latency by 35%."
                  }
                }
              ]
            }
            """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(providerJsonResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AiEnhanceRequest request = new AiEnhanceRequest("experience", "Worked on React app to make it faster.", "Senior Frontend Engineer", "React, TypeScript");
        AiEnhanceResponse response = service.enhanceContent(request);

        assertNotNull(response);
        assertEquals("experience", response.getField());
        assertEquals("Engineered high-performance React frontends reducing render latency by 35%.", response.getEnhancedContent());
        assertEquals("gpt-4o-mini", response.getModel());
        assertEquals("openai", response.getProvider());
    }

    @Test
    @DisplayName("Should handle 401 provider authentication failure")
    @SuppressWarnings("unchecked")
    void enhanceContent_AuthenticationFailure_ThrowsBadGateway() throws Exception {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "sk-invalid-key", "gpt-4o-mini", 30, mockHttpClient, objectMapper);

        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AiEnhanceRequest request = new AiEnhanceRequest("summary", "Valid summary content.");
        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.enhanceContent(request));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertEquals("AUTHENTICATION_FAILED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should handle 429 provider rate limit error")
    @SuppressWarnings("unchecked")
    void enhanceContent_RateLimit_ThrowsTooManyRequests() throws Exception {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "sk-valid-key", "gpt-4o-mini", 30, mockHttpClient, objectMapper);

        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        AiEnhanceRequest request = new AiEnhanceRequest("summary", "Valid summary content.");
        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.enhanceContent(request));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals("RATE_LIMIT_EXCEEDED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should handle request timeout properly")
    @SuppressWarnings("unchecked")
    void enhanceContent_Timeout_ThrowsGatewayTimeout() throws Exception {
        AIServiceImpl service = new AIServiceImpl("openai", "https://api.openai.com/v1", "sk-valid-key", "gpt-4o-mini", 5, mockHttpClient, objectMapper);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("Request timed out"));

        AiEnhanceRequest request = new AiEnhanceRequest("summary", "Valid summary content.");
        AiServiceException ex = assertThrows(AiServiceException.class, () -> service.enhanceContent(request));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertEquals("TIMEOUT", ex.getErrorCode());
    }
}
