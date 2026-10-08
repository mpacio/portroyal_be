package com.matteopaciolla.prbe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Consistent JSON error envelope returned with the corresponding HTTP error status.")
public class ErrorResponse {

    @Schema(description = "HTTP status code.", example = "400")
    private int status;

    @Schema(description = "HTTP reason phrase.", example = "Bad Request")
    private String error;

    @Schema(description = "Human-readable description of the failure.", example = "Invalid input")
    private String message;

    @Schema(description = "Stable, machine-readable status code.", example = "HTTP_400")
    private String errorCode;

    @Schema(description = "Structured details about fields or parameters that caused the error.")
    private List<ErrorDetail> errorDetails;

    @Schema(description = "Additional structured context for the error.")
    private Map<String, Object> errorContext;

    @Schema(description = "Documentation or support links, when available.")
    private List<String> errorLinks;

    @Schema(description = "UTC time at which the error response was created.", example = "2026-10-08T09:23:03Z")
    private String timestamp;

    private String description;
    private String suggestion;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
        initializeMetadata();
    }

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        initializeMetadata();
    }

    public ErrorResponse(int status, String error, String message, List<ErrorDetail> errorDetails) {
        this.status = status;
        this.error = error;
        this.message = message;
        initializeMetadata();
        setErrorDetails(errorDetails);
    }

    public ErrorResponse(int status, String error, String message, String description, String suggestion) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.description = description;
        this.suggestion = suggestion;
        initializeMetadata();
    }

    public void setErrorDetails(List<ErrorDetail> errorDetails) {
        this.errorDetails = errorDetails;
        this.errorContext = errorDetails == null ? Map.of() : Map.of("details", errorDetails);
    }

    private void initializeMetadata() {
        this.errorCode = "HTTP_" + status;
        this.timestamp = Instant.now().toString();
        this.errorContext = Map.of();
        this.errorLinks = List.of();
        if (message == null || message.isBlank()) {
            message = error;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String key;
        private String value;

        public ErrorDetail(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}