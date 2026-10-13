package com.anurag.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an AI provider call fails, is unconfigured, or times out.
 */
public class AiServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AiServiceException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "AI_ERROR";
    }

    public AiServiceException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
