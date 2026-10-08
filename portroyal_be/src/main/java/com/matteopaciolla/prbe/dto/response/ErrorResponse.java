package com.matteopaciolla.prbe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Consistent JSON error response for REST API requests.")
public class ErrorResponse {

    @Schema(description = "HTTP status code.", example = "400")
    private int status;

    @Schema(description = "Stable machine-readable status code.", example = "BAD_REQUEST")
    private String code;

    @Schema(description = "Human-readable error message.", example = "Invalid request.")
    private String message;

    @Schema(description = "Optional validation or domain-specific error context.")
    private List<ErrorDetail> details;

    @Schema(description = "UTC time at which the error was created.", example = "2026-10-08T08:17:27Z")
    private Instant timestamp;

    public ErrorResponse(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ErrorResponse(HttpStatus status, String message, List<ErrorDetail> details) {
        this(status.value(), status.name(), message, details, Instant.now());
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "A field-specific or domain-specific error detail.")
    public static class ErrorDetail {
        @Schema(description = "Field or context key.")
        private String field;

        @Schema(description = "Detail message.")
        private String message;
    }
}
