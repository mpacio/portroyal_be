package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.config.WebConfig;
import com.matteopaciolla.prbe.config.WebSecurityConfig;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
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

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserController.class,
        PublicController.class,
        MatchController.class,
        GameController.class,
        CardController.class,
        SentinelController.class
})
@Import({WebConfig.class, WebSecurityConfig.class})
class ApiRouteContractTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private GameService gameService;

    @MockBean
    private SentinelService sentinelService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void routesUseCanonicalResourcePathsAndHttpMethods() {
        Set<String> routes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream()
                        .flatMap(path -> mapping.getMethodsCondition().getMethods().stream()
                                .map(method -> method.name() + " " + path)))
                .collect(Collectors.toSet());

        assertThat(routes).contains(
                "GET /api/v1/users",
                "PATCH /api/v1/users/{username}",
                "DELETE /api/v1/users/{username}",
                "POST /api/v1/public/users",
                "POST /api/v1/public/email-confirmations",
                "POST /api/v1/matches",
                "POST /api/v1/matches/{match-key}/players",
                "GET /api/v1/matches/{match-key}",
                "GET /api/v1/matches/current",
                "POST /api/v1/matches/current/moves",
                "GET /api/v1/matches/{match-key}/moves",
                "GET /api/v1/matches/{match-key}/moves/{move-number}",
                "GET /api/v1/cards",
                "GET /api/v1/cards/contracts",
                "POST /api/v1/sentinel/subscriptions/callbacks"
        ).doesNotContain(
                "POST /api/v1/public/register",
                "GET /api/v1/user",
                "PUT /api/v1/match/join",
                "POST /api/v1/game/move",
                "GET /api/v1/card"
        );
    }

    @Test
    void userLookupAcceptsUnderscoredQueryParameterAndRejectsOldCamelCaseName() throws Exception {
        when(userService.getUserByTelegramId("123456789")).thenReturn(new UserDto());

        mockMvc.perform(get("/api/v1/users")
                        .param("telegram_id", "123456789")
                        .with(user("alice")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users")
                        .param("telegramId", "123456789")
                        .with(user("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("HTTP_400"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unauthenticatedApiErrorsUseConsistentJsonEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("HTTP_401"))
                .andExpect(jsonPath("$.errorContext").isMap())
                .andExpect(jsonPath("$.errorLinks").isArray())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void telegramAccountEndpointsRemainBotOnly() throws Exception {
        mockMvc.perform(post("/api/v1/users/telegram-accounts")
                        .contentType("application/json")
                        .content("{}")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("HTTP_403"));

        mockMvc.perform(post("/api/v1/users/identity-unifications")
                        .contentType("application/json")
                        .content("{}")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("HTTP_403"));
    }

    @Test
    void telegramAccountRegistrationStillAllowsBotRole() throws Exception {
        when(userService.registerUser(any(UserReqDto.class), eq(true))).thenReturn(new UserDto());

        mockMvc.perform(post("/api/v1/users/telegram-accounts")
                        .contentType("application/json")
                        .content("{\"username\":\"tg-user\",\"telegramId\":\"123456789\"}")
                        .with(user("shaslabot").roles("BOT")))
                .andExpect(status().isCreated());
    }
}
