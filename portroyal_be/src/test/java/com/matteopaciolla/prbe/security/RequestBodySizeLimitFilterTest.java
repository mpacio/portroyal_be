package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RequestBodySizeLimitFilterTest {

    @Test
    void rejectsJsonBodyLargerThanConfiguredMaximum() throws Exception {
        RequestBodySizeLimitFilter filter = new RequestBodySizeLimitFilter(2, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/register");
        request.setContentType("application/json");
        request.setContent(new byte[]{'{', '}', ' '});
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> continued.set(true));

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("configured maximum size");
        assertThat(continued).isFalse();
    }

    @Test
    void rejectsChunkedJsonBodyWhenItExceedsConfiguredMaximum() throws Exception {
        RequestBodySizeLimitFilter filter = new RequestBodySizeLimitFilter(2, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/register") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        request.setContent(new byte[]{'{', '}', ' '});
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> continued.set(true));

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(continued).isFalse();
    }

    @Test
    void allowsBodyAtTheConfiguredMaximumAndReplaysItForTheController() throws Exception {
        RequestBodySizeLimitFilter filter = new RequestBodySizeLimitFilter(2, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/register");
        request.setContentType("application/json");
        request.setContent(new byte[]{'{', '}'});
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(servletRequest.getInputStream().readAllBytes()).containsExactly('{', '}');
            continued.set(true);
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(continued).isTrue();
    }
}
