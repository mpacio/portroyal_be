package com.matteopaciolla.prbe.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ErrorResponseMessageConverter implements HttpMessageConverter<ErrorResponse> {

    private final ObjectMapper objectMapper;

    public ErrorResponseMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false; // We only need to write ErrorResponse
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return ErrorResponse.class.isAssignableFrom(clazz) && MediaType.TEXT_EVENT_STREAM.includes(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
    }

    @Override
    public ErrorResponse read(Class<? extends ErrorResponse> clazz, HttpInputMessage inputMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(ErrorResponse errorResponse, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(("data:" + objectMapper.writeValueAsString(errorResponse) + "\n\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}