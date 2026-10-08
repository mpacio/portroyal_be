package com.matteopaciolla.prbe.controller.handler;

import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.exceptions.common.UniqueConstraintViolatedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException e) {
        log.debug("Captured HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return error(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException e) {
        log.debug("Captured MissingServletRequestParameterException: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        List<ErrorResponse.ErrorDetail> details = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ErrorDetail(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        log.debug("Captured MethodArgumentNotValidException: {}", details);
        return error(HttpStatus.BAD_REQUEST, "Invalid request.", details);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Captured NoResourceFoundException: {}", e.getMessage());
        return error(HttpStatus.NOT_FOUND, "Resource not found.");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        List<ErrorResponse.ErrorDetail> details = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(validationError -> new ErrorResponse.ErrorDetail(
                                Objects.toString(result.getMethodParameter().getParameterName(), "request"),
                                validationError.getDefaultMessage())))
                .collect(Collectors.toList());
        log.debug("Captured HandlerMethodValidationException: {}", details);
        return error(HttpStatus.BAD_REQUEST, "Invalid request.", details);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.debug("Captured HttpMediaTypeNotSupportedException: {}", e.getMessage());
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("Captured HttpMessageNotReadableException: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or invalid.");
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ResponseEntity<ErrorResponse> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("Captured AsyncRequestTimeoutException: {}", e.getMessage());
        return error(HttpStatus.REQUEST_TIMEOUT, "Request timed out.");
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        log.error("Captured IOException: {}", e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.debug("Captured ResourceNotFoundException: {}", e.getMessage());
        return error(HttpStatus.NOT_FOUND, e.getMessage(),
                List.of(new ErrorResponse.ErrorDetail("resource", e.getDescription())));
    }

    @ExceptionHandler(UniqueConstraintViolatedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleUniqueConstraintViolatedException(UniqueConstraintViolatedException e) {
        log.debug("Captured UniqueConstraintViolatedException: {}", e.getMessage());
        List<ErrorResponse.ErrorDetail> details = new ArrayList<>();
        if (e.getDescription() != null) {
            details.add(new ErrorResponse.ErrorDetail("description", e.getDescription()));
        }
        if (e.getSuggestion() != null) {
            details.add(new ErrorResponse.ErrorDetail("suggestion", e.getSuggestion()));
        }
        return error(HttpStatus.CONFLICT, e.getMessage(), details);
    }

    @ExceptionHandler(BaseClientCausedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBaseClientCausedException(BaseClientCausedException e) {
        log.debug("Captured BaseClientCausedException: {}", e.getMessage());
        List<ErrorResponse.ErrorDetail> details = new ArrayList<>();
        if (e.getDetails() != null) {
            e.getDetails().forEach((field, message) ->
                    details.add(new ErrorResponse.ErrorDetail(field, message)));
        }
        if (e.getDescription() != null) {
            details.add(new ErrorResponse.ErrorDetail("description", e.getDescription()));
        }
        if (e.getSuggestion() != null) {
            details.add(new ErrorResponse.ErrorDetail("suggestion", e.getSuggestion()));
        }
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), details);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGeneralExceptions(Exception e) {
        log.error("Captured Exception: {}", e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return error(status, message, null);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            List<ErrorResponse.ErrorDetail> details) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(status, message, details));
    }
}
