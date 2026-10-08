package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.MatchDto;
import com.matteopaciolla.prbe.dto.response.MatchResponse;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.service.MatchService;
import com.matteopaciolla.prbe.service.SentinelService;
import com.matteopaciolla.prbe.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchService matchService;
    @Mock
    private UserService userService;
    @Mock
    private SentinelService sentinelService;

    @InjectMocks
    private MatchController matchController;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice", "password", List.of(new SimpleGrantedAuthority(UserRole.USER.toString()))));
        user = new UserEntity("alice", "encoded", List.of(UserRole.USER));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMatchStatus_whenMoveNumberMatchesCurrentCount_returnsNoNewMoves() {
        when(userService.getUserEntityByUsername("alice")).thenReturn(user);
        MatchDto matchDto = new MatchDto();
        matchDto.setMovesCount(5);
        when(matchService.getPlayingMatch(user, false)).thenReturn(Optional.of(matchDto));

        ResponseEntity<MatchResponse> response = matchController.getMatchStatus(null, 5);

        assertThat(response.getBody().getMessage()).isEqualTo("No new moves");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void getMatchStatus_whenMoveNumberDiffers_returnsCurrentMatch() {
        when(userService.getUserEntityByUsername("alice")).thenReturn(user);
        MatchDto matchDto = new MatchDto();
        matchDto.setMovesCount(5);
        when(matchService.getPlayingMatch(user, false)).thenReturn(Optional.of(matchDto));

        ResponseEntity<MatchResponse> response = matchController.getMatchStatus(null, 4);

        assertThat(response.getBody().getData()).isSameAs(matchDto);
    }

    @Test
    void getMatchStatus_whenBotAuthenticated_resolvesPlayerFromTelegramId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "shaslabot", "password", List.of(new SimpleGrantedAuthority(UserRole.BOT.toString()))));
        when(userService.getUserEntityByTelegramId("123456789")).thenReturn(user);
        when(matchService.getPlayingMatch(user, true)).thenReturn(Optional.empty());

        matchController.getMatchStatus("123456789", null);

        verify(userService).getUserEntityByTelegramId("123456789");
        verify(matchService).getPlayingMatch(user, true);
    }

    @Test
    void joinMatch_whenBotAuthenticated_resolvesActingUserFromTelegramId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "shaslabot", "password", List.of(new SimpleGrantedAuthority(UserRole.BOT.toString()))));
        when(userService.getUserEntityByTelegramId("123456789")).thenReturn(user);

        matchController.joinMatch("ABC123", "123456789");

        verify(userService).getUserEntityByTelegramId("123456789");
        verify(userService, never()).getUserEntityByUsername("shaslabot");
        verify(matchService).joinMatch("ABC123", user);
    }
}
