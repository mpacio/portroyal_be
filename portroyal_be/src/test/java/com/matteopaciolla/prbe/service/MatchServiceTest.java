package com.matteopaciolla.prbe.service;

import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.MatchInfoDto;
import com.matteopaciolla.prbe.dto.request.AddAiPlayerReqDto;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.exceptions.match.MatchCompositionLockedException;
import com.matteopaciolla.prbe.exceptions.match.MatchFullException;
import com.matteopaciolla.prbe.model.entity.AIPlayerEntity;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.ConfigPropertyRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.cachingproxy.MatchRetainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private ConfigPropertyRepository configPropertyRepository;
    @Mock
    private MatchRetainer matchRetainer;
    @Mock
    private GameService gameService;

    @InjectMocks
    private MatchService matchService;

    private UserEntity host;
    private MatchEntity matchEntity;

    @BeforeEach
    void setUp() {
        host = new UserEntity("alice", "encoded", List.of(UserRole.USER));
        host.setId(1L);
        matchEntity = new MatchEntity();
        matchEntity.setKeyCode("ABC123");
        matchEntity.setHostUser(host);
        matchEntity.setStarted(false);
        matchEntity.setEnded(false);
        matchEntity.setPlayers(new ArrayList<>(List.of(host)));
    }

    private AddAiPlayerReqDto addAiPlayerReqDto(BotDifficulty difficulty, String name) {
        AddAiPlayerReqDto dto = new AddAiPlayerReqDto();
        dto.setDifficulty(difficulty);
        dto.setName(name);
        return dto;
    }

    @Test
    void addAiPlayer_happyPath_createsAndAppendsAnAiPlayer() {
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));
        when(matchRepository.save(any(MatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchInfoDto result = matchService.addAiPlayer(host, addAiPlayerReqDto(BotDifficulty.MEDIUM, "Rocco"));

        assertThat(result.getKeyCode()).isEqualTo("ABC123");
        assertThat(matchEntity.getAiPlayers()).hasSize(1);
        AIPlayerEntity savedAiPlayer = matchEntity.getAiPlayers().get(0);
        assertThat(savedAiPlayer.getUsername()).isEqualTo("ABC123-ai-1");
        assertThat(savedAiPlayer.getDifficulty()).isEqualTo(BotDifficulty.MEDIUM);
        assertThat(savedAiPlayer.getDisplayName()).isEqualTo("Rocco");
        assertThat(savedAiPlayer.getMatch()).isSameAs(matchEntity);
        assertThat(matchEntity.getPlayerCount()).isEqualTo(2);
    }

    @Test
    void addAiPlayer_onStartedMatch_throwsMatchCompositionLockedException() {
        matchEntity.setStarted(true);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.addAiPlayer(host, addAiPlayerReqDto(BotDifficulty.EASY, null)))
                .isInstanceOf(MatchCompositionLockedException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void addAiPlayer_onFullMatch_throwsMatchFullException() {
        List<UserEntity> fullPlayers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fullPlayers.add(new UserEntity("player" + i, "pwd", List.of(UserRole.USER)));
        }
        matchEntity.setPlayers(fullPlayers);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.addAiPlayer(host, addAiPlayerReqDto(BotDifficulty.EASY, null)))
                .isInstanceOf(MatchFullException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void addAiPlayer_hostNotHostingAnOpenMatch_throwsResourceNotFoundException() {
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.addAiPlayer(host, addAiPlayerReqDto(BotDifficulty.EASY, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeAiPlayer_happyPath_removesTheAiPlayerFromTheMatch() {
        AIPlayerEntity aiPlayer = new AIPlayerEntity("ABC123-ai-1", "AI Player 1", BotDifficulty.HARD);
        aiPlayer.setId(2L);
        matchEntity.addAiPlayer(aiPlayer);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        String keyCode = matchService.removeAiPlayer(host, "ABC123-ai-1");

        assertThat(keyCode).isEqualTo("ABC123");
        assertThat(matchEntity.getAiPlayers()).isEmpty();
        assertThat(matchEntity.getPlayers()).extracting(UserEntity::getUsername).containsExactly("alice");
        verify(matchRepository).save(matchEntity);
    }

    @Test
    void removeAiPlayer_onStartedMatch_throwsMatchCompositionLockedException() {
        matchEntity.setStarted(true);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.removeAiPlayer(host, "ABC123-ai-1"))
                .isInstanceOf(MatchCompositionLockedException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void removeAiPlayer_nonExistentOrNonAiUsername_throwsResourceNotFoundException() {
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.removeAiPlayer(host, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(matchRepository, never()).save(any());
    }
}
