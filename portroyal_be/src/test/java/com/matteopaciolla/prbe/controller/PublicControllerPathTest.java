package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.response.MatchInfosPageResponse;
import com.matteopaciolla.prbe.config.WebConfig;
import com.matteopaciolla.prbe.config.WebSecurityConfig;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.service.GameService;
import com.matteopaciolla.prbe.service.MatchService;
import com.matteopaciolla.prbe.service.SentinelService;
import com.matteopaciolla.prbe.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {
        PublicController.class,
        com.matteopaciolla.prbe.controller.pages.PublicController.class,
        UserController.class,
        MatchController.class,
        GameController.class,
        CardController.class,
        SentinelController.class
})
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

    @MockBean
    private MatchService matchService;

    @MockBean
    private GameService gameService;

    @MockBean
    private SentinelService sentinelService;

    @Test
    void publicJsonResourcesAreVersionedAndHtmlConfirmationIsNot() {
        Set<String> paths = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .collect(Collectors.toSet());

        assertThat(paths)
                .contains("/api/v1/users",
                        "/api/v1/email-confirmations",
                        "/api/v1/telegram-account-verifications",
                        "/api/v1/users/me",
                        "/api/v1/users/identity",
                        "/api/v1/users/telegram",
                        "/api/v1/users/telegram-account",
                        "/api/v1/users/{username}",
                        "/api/v1/matches",
                        "/api/v1/matches/current",
                        "/api/v1/matches/current/status",
                        "/api/v1/matches/current/moves",
                        "/api/v1/cards",
                        "/api/v1/contract-cards",
                        "/api/v1/matches/{keyCode}/alerts",
                        "/public/confirmEmail")
                .doesNotContain("/api/v1/public/register",
                        "/api/v1/public/newEmailConfirmation",
                        "/api/v1/public/newTgUnifyEmailOtp",
                        "/api/v1/user/me",
                        "/api/v1/match/status",
                        "/api/v1/game/move",
                        "/api/v1/card",
                        "/api/v1/sentinel/long-polling/subscribe",
                        "/api/v1/public/confirmEmail");
    }

    @Test
    void registrationUsesCanonicalCollectionPathAndOldPathIsGone() throws Exception {
        when(userService.registerUser(any(UserReqDto.class), eq(false))).thenReturn(new UserDto());
        String payload = "{\"username\":\"alice42\"}";

        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/public/register")
                        .with(user("alice42").roles("USER"))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedApiErrorsUseTheStructuredJsonEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required."))
                .andExpect(header().string("WWW-Authenticate",
                        "Basic realm=\"PortRoyal API\", charset=\"UTF-8\""))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void invalidJsonErrorsUseTheStructuredJsonEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is missing or invalid."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void missingCardErrorsUseTheStructuredJsonEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/cards/999999")
                        .with(user("alice42").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.details[0].field").value("resource"));
    }

    @Test
    void matchListUsesUnderscoreQueryParameterNames() throws Exception {
        when(matchService.getMatchesByPlayer(isNull(), isNull(), eq(1), eq(7),
                eq(MatchRepository.SortField.CREATED_AT), eq(Sort.Direction.ASC)))
                .thenReturn(new MatchInfosPageResponse(1, 7, 0, 0, "CREATED_AT", "ASC", java.util.List.of()));

        mockMvc.perform(get("/api/v1/matches")
                        .with(user("alice42").roles("USER"))
                        .param("page_number", "1")
                        .param("page_size", "7")
                        .param("sort_field", "CREATED_AT")
                        .param("sort_direction", "ASC"))
                .andExpect(status().isOk());

        verify(matchService).getMatchesByPlayer(null, null, 1, 7,
                MatchRepository.SortField.CREATED_AT, Sort.Direction.ASC);
    }

    @Test
    void telegramIdentityRoutesRemainBotOnly() throws Exception {
        mockMvc.perform(post("/api/v1/users/telegram")
                        .with(user("alice42").roles("USER"))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/v1/users/telegram-account")
                        .with(user("alice42").roles("USER"))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
