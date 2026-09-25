package com.matteopaciolla.prbe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private List<ErrorDetail> errorDetails;
    private String description;
    private String suggestion;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
    }

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(int status, String error, String message, List<ErrorDetail> errorDetails) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.errorDetails = errorDetails;
    }

    public ErrorResponse(int status, String error, String message, String description, String suggestion) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.description = description;
        this.suggestion = suggestion;
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String key;
        private String value;
    }
}