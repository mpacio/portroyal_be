package com.matteopaciolla.prbe.controller.handler;

import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import com.matteopaciolla.prbe.exceptions.*;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    // -------------------------------- Spring Exceptions ---------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException e) {
        log.debug("Captured HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException e) {
        log.debug("Captured MissingServletRequestParameterException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.BAD_REQUEST);
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
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Invalid input",
                details
        ), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Captured NoResourceFoundException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        List<ErrorResponse.ErrorDetail> details = e.getAllValidationResults().stream()
                .map(vr -> {
                    String key = vr.getMethodParameter().toString();
                    String value = vr.getResolvableErrors().getFirst().getDefaultMessage();
                    return new ErrorResponse.ErrorDetail(key, value);
                })
                .collect(Collectors.toList());
        log.debug("Captured HandlerMethodValidationException: {}", details);
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Invalid input",
                details
        ), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.debug("Captured HttpMediaTypeNotSupportedException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("Captured HttpMessageNotReadableException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ResponseEntity<ErrorResponse> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("Captured AsyncRequestTimeoutException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.REQUEST_TIMEOUT.value(),
                HttpStatus.REQUEST_TIMEOUT.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        if (e.getMessage().equals("An established connection was aborted by the software in your host machine")) {
            log.error("Captured IOException: {}", e.getMessage());
        } else {
            log.error("Captured IOException: {}", e.getMessage(), e);
        }
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // -------------------------------- Custom Exceptions ---------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.debug("Captured ResourceNotFoundException: {}", e.getMessage());
        return new ResponseEntity<>(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                e.getMessage()
        ), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BaseClientCausedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBaseClientCausedException(BaseClientCausedException e) {
        log.debug("Captured BaseClientCausedException: {}", e.getMessage());
        ErrorResponse res = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                e.getMessage(),
                e.getDescription(),
                e.getSuggestion()
        );
        if (e.getDetails() != null) {
            res.setErrorDetails(e.getDetails().entrySet().stream()
                    .map(entry -> new ErrorResponse.ErrorDetail(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
        }
        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }

    // -------------------------------- General Exception Handler ---------------------------------

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGeneralExceptions(Exception e) {
        log.error("Captured Exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error: " + e.getMessage()));
    }
}
