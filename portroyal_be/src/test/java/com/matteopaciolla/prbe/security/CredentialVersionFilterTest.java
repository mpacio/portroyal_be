package com.matteopaciolla.prbe.security;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredentialVersionFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidatesOldLoginSessionAfterCredentialVersionChanges() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        UserEntity user = new UserEntity("alice42", "encoded", List.of(UserRole.USER));
        user.setId(42L);
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        user.setCredentialVersion(2);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "alice42", null, List.of());
        authentication.setDetails(new CredentialVersion(42L, 1));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        AtomicBoolean continued = new AtomicBoolean();

        new CredentialVersionFilter(userRepository).doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> continued.set(true));

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(continued).isTrue();
    }
}
