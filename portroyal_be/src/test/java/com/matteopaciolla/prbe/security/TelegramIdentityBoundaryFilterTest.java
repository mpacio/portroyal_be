package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramIdentityBoundaryFilterTest {

    private final TelegramIdentityBoundaryFilter filter =
            new TelegramIdentityBoundaryFilter(new ObjectMapper());

    @Test
    void rejectsTelegramIdentityOnOrdinaryGameRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/game/move");
        request.addHeader("tgId", "123456789");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).doesNotContain("123456789");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void allowsTelegramIdentityOnlyOnAccountLinkingRoutes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/user/unify");
        request.addHeader("tgId", "123456789");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void rejectsTelegramIdentityAsAnOrdinaryApiParameter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/match/status");
        request.setParameter("tgId", "123456789");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsTelegramIdentityParameterOnAccountLinkingRoute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/user/unify");
        request.setParameter("tgId", "123456789");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(chain.getRequest()).isNull();
    }

}
