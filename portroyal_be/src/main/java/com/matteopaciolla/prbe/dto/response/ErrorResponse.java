package com.matteopaciolla.prbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
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
        this.errorDetails = List.of();
    }

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.errorDetails = List.of();
    }

    public ErrorResponse(int status, String error, String message, List<ErrorDetail> errorDetails) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.errorDetails = errorDetails == null ? List.of() : errorDetails;
    }

    public ErrorResponse(int status, String error, String message, String description, String suggestion) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.errorDetails = List.of();
        this.description = description;
        this.suggestion = suggestion;
    }

    @Data
    @AllArgsConstructor
    public static class ErrorDetail {
        private String key;
        private String value;
    }
}