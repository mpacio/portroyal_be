package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.config.WebConfig;
import com.matteopaciolla.prbe.config.WebSecurityConfig;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {PublicApiController.class, PublicController.class})
@Import({WebConfig.class, WebSecurityConfig.class})
class PublicControllerPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void jsonPublicEndpointsAreVersionedAndHtmlConfirmationIsNot() {
        Set<String> paths = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .collect(Collectors.toSet());

        assertThat(paths)
                .contains("/api/v1/public/register",
                        "/api/v1/public/newEmailConfirmation",
                        "/api/v1/public/newTgUnifyEmailOtp",
                        "/public/confirmEmail")
                .doesNotContain("/public/register",
                        "/public/newEmailConfirmation",
                        "/public/newTgUnifyEmailOtp",
                        "/api/v1/public/confirmEmail");
    }

    @Test
    void registrationIsPublicAtVersionedPathAndUnavailableAtOldPath() throws Exception {
        when(userService.registerUser(any(UserReqDto.class), eq(false))).thenReturn(new UserDto());
        String payload = "{\"username\":\"alice42\"}";

        mockMvc.perform(post("/api/v1/public/register")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/public/register")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailConfirmationPageRemainsPublicAtUnversionedPath() throws Exception {
        UserDto user = new UserDto();
        user.setUsername("alice42");
        when(userService.confirmEmail("token")).thenReturn(user);

        mockMvc.perform(get("/public/confirmEmail").param("token", "token"))
                .andExpect(status().isOk())
                .andExpect(view().name("email_confirmed_page"));
    }
}
