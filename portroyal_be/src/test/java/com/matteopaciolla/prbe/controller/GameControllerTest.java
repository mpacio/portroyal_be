package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.request.MoveReqDto;
import com.matteopaciolla.prbe.dto.response.MoveResponse;
import com.matteopaciolla.prbe.exceptions.common.MandatoryBotParamException;
import com.matteopaciolla.prbe.service.GameService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameController gameController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void move_whenBotAuthenticated_resolvesMoveByTelegramId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "shaslabot", "password", List.of(new SimpleGrantedAuthority(UserRole.BOT.toString()))));
        MoveReqDto moveReqDto = new MoveReqDto("DISCOVER", -1, -1, List.of());
        MoveResponse moveResponse = new MoveResponse("Move added successfully", null);
        when(gameService.insertMoveWithTelegramId("123456789", moveReqDto)).thenReturn(moveResponse);

        ResponseEntity<MoveResponse> response = gameController.move("123456789", moveReqDto);

        verify(gameService).insertMoveWithTelegramId("123456789", moveReqDto);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getStatus()).isEqualTo(201);
    }

    @Test
    void move_whenBotAuthenticatedWithoutTelegramId_rejectsRequest() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "shaslabot", "password", List.of(new SimpleGrantedAuthority(UserRole.BOT.toString()))));

        assertThatThrownBy(() -> gameController.move(null, new MoveReqDto("DISCOVER", -1, -1, List.of())))
                .isInstanceOf(MandatoryBotParamException.class);
    }
}
