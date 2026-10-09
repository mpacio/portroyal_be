package com.matteopaciolla.prbe.controller.handler;

import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import com.matteopaciolla.prbe.exceptions.BaseInternalException;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE =
            "An unexpected error occurred while processing the request. Please try again later.";

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException e) {
        log.debug("Captured HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return response(HttpStatus.METHOD_NOT_ALLOWED,
                "The HTTP method '" + e.getMethod() + "' is not supported for this endpoint.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException e) {
        log.debug("Captured MissingServletRequestParameterException: {}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST,
                "The required request parameter '" + e.getParameterName() + "' is missing.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        List<ErrorResponse.ErrorDetail> details = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ErrorDetail(
                        error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
                .toList();
        log.debug("Captured MethodArgumentNotValidException: {}", details);
        return response(HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid. Correct the fields listed in errorDetails and try again.",
                details);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Captured NoResourceFoundException: {}", e.getMessage());
        return response(HttpStatus.NOT_FOUND, "The requested endpoint was not found.",
                List.of(new ErrorResponse.ErrorDetail("path", e.getResourcePath())));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        List<ErrorResponse.ErrorDetail> details = e.getAllValidationResults().stream()
                .flatMap(result -> {
                    String parameterName = Objects.requireNonNullElse(
                            result.getMethodParameter().getParameterName(), "request parameter");
                    return result.getResolvableErrors().stream()
                            .map(error -> new ErrorResponse.ErrorDetail(
                                    parameterName, Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")));
                })
                .toList();
        log.debug("Captured HandlerMethodValidationException: {}", details);
        return response(HttpStatus.BAD_REQUEST,
                "One or more request parameters are invalid. Correct the values listed in errorDetails and try again.",
                details);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.debug("Captured HttpMediaTypeNotSupportedException: {}", e.getMessage());
        String mediaType = e.getContentType() == null ? "the supplied media type" : "'" + e.getContentType() + "'";
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "The request content type " + mediaType + " is not supported for this endpoint.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("Captured HttpMessageNotReadableException: {}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST,
                "The request body is missing or malformed. Ensure it is valid JSON and matches the expected format.");
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("Captured AsyncRequestTimeoutException: {}", e.getMessage());
        return response(HttpStatus.REQUEST_TIMEOUT,
                "The request timed out before it could be completed. Please try again.");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        if ("An established connection was aborted by the software in your host machine".equals(e.getMessage())) {
            log.error("Captured IOException: {}", e.getMessage());
        } else {
            log.error("Captured IOException: {}", e.getMessage(), e);
        }
        return internalError();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.debug("Captured ResourceNotFoundException: {}", e.getMessage());
        return response(HttpStatus.NOT_FOUND, "The requested resource could not be found.",
                List.of(), e.getDescription(), null);
    }

    @ExceptionHandler(BaseClientCausedException.class)
    public ResponseEntity<ErrorResponse> handleBaseClientCausedException(BaseClientCausedException e) {
        log.debug("Captured BaseClientCausedException: {}", e.getMessage());
        List<ErrorResponse.ErrorDetail> details = e.getDetails() == null
                ? List.of()
                : e.getDetails().entrySet().stream()
                        .map(entry -> new ErrorResponse.ErrorDetail(entry.getKey(), entry.getValue()))
                        .toList();
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "The request could not be processed because of invalid input.";
        }
        return response(HttpStatus.BAD_REQUEST, message, details, e.getDescription(), e.getSuggestion());
    }

    @ExceptionHandler(BaseInternalException.class)
    public ResponseEntity<ErrorResponse> handleBaseInternalException(BaseInternalException e) {
        log.error("Captured BaseInternalException: {}", e.getMessage(), e);
        return internalError();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralExceptions(Exception e) {
        log.error("Captured Exception: {}", e.getMessage(), e);
        return internalError();
    }

    private ResponseEntity<ErrorResponse> internalError() {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        return response(status, message, List.of(), null, null);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status, String message, List<ErrorResponse.ErrorDetail> details) {
        return response(status, message, details, null, null);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String message,
            List<ErrorResponse.ErrorDetail> details,
            String description,
            String suggestion) {
        ErrorResponse body = new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, details, description, suggestion);
        return ResponseEntity.status(status).body(body);
    }
}
