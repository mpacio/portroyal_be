package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RequiredArgsConstructor
public class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    private final int maxBytes;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain) throws ServletException, IOException {
        if (maxBytes <= 0) {
            throw new IllegalStateException("The maximum request body size must be positive");
        }
        if (!isJsonContent(request.getContentType()) || request.getContentLengthLong() == 0) {
            filterChain.doFilter(request, response);
            return;
        }
        if (request.getContentLengthLong() > maxBytes) {
            writeTooLargeResponse(response);
            return;
        }

        byte[] body = request.getInputStream().readNBytes(maxBytes + 1);
        if (body.length > maxBytes) {
            writeTooLargeResponse(response);
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private static boolean isJsonContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return mediaType.equals("application/json") || mediaType.endsWith("+json");
    }

    private void writeTooLargeResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                        "Request body exceeds the configured maximum size"));
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;
        private ServletInputStream inputStream;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            if (inputStream == null) {
                ByteArrayInputStream input = new ByteArrayInputStream(body);
                inputStream = new ServletInputStream() {
                    @Override
                    public int read() {
                        return input.read();
                    }

                    @Override
                    public boolean isFinished() {
                        return input.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {
                        try {
                            if (input.available() > 0) {
                                readListener.onDataAvailable();
                            }
                            if (input.available() == 0) {
                                readListener.onAllDataRead();
                            }
                        } catch (IOException e) {
                            readListener.onError(e);
                        }
                    }
                };
            }
            return inputStream;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
