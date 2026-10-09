package com.matteopaciolla.prbe.controller.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import com.matteopaciolla.prbe.exceptions.BaseInternalException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void methodNotAllowedUsesConsistentResponseAndExplainsTheProblem() throws Exception {
        ErrorResponse response = handler.handleMethodNotAllowedException(
                new HttpRequestMethodNotSupportedException("PATCH")).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(response.getError()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
        assertThat(response.getMessage()).contains("PATCH").contains("not supported");
        assertConsistentJsonShape(response);
    }

    @Test
    void internalErrorsDoNotExposeImplementationDetails() throws Exception {
        ErrorResponse response = handler.handleBaseInternalException(
                new BaseInternalException("database password or SQL details")).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getMessage()).contains("unexpected error").doesNotContain("database password");
        assertConsistentJsonShape(response);
    }

    private void assertConsistentJsonShape(ErrorResponse response) throws Exception {
        JsonNode json = new ObjectMapper().valueToTree(response);
        assertThat(json.has("status")).isTrue();
        assertThat(json.has("error")).isTrue();
        assertThat(json.has("message")).isTrue();
        assertThat(json.has("errorDetails")).isTrue();
        assertThat(json.get("errorDetails").isArray()).isTrue();
        assertThat(json.has("description")).isTrue();
        assertThat(json.has("suggestion")).isTrue();
    }
}
