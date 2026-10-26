package com.matteopaciolla.prbe.security;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    @Test
    void generatesResponseIdAndMakesItAvailableInMdcDuringRequest() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/me");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "client-controlled-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> loggedRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                loggedRequestId.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .matches("[0-9a-f-]{36}")
                .isNotEqualTo("client-controlled-value");
        assertThat(loggedRequestId.get()).isEqualTo(response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
