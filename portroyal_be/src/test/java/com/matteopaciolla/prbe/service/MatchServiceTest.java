package com.matteopaciolla.prbe.service;

import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.MatchInfoDto;
import com.matteopaciolla.prbe.dto.request.AddBotReqDto;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.exceptions.match.MatchCompositionLockedException;
import com.matteopaciolla.prbe.exceptions.match.MatchFullException;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.ConfigPropertyRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.repository.cachingproxy.MatchRetainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private ConfigPropertyRepository configPropertyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
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

    private AddBotReqDto addBotReqDto(BotDifficulty difficulty, String name) {
        AddBotReqDto dto = new AddBotReqDto();
        dto.setDifficulty(difficulty);
        dto.setName(name);
        return dto;
    }

    @Test
    void addBot_happyPath_createsAndAppendsABotPlayer() {
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.save(any(MatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchInfoDto result = matchService.addBot(host, addBotReqDto(BotDifficulty.MEDIUM, "Rocco"));

        assertThat(result.getKeyCode()).isEqualTo("ABC123");
        ArgumentCaptor<UserEntity> savedBotCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedBotCaptor.capture());
        UserEntity savedBot = savedBotCaptor.getValue();
        assertThat(savedBot.getUsername()).isEqualTo("ABC123-bot-1");
        assertThat(savedBot.getRoles()).containsExactly(UserRole.AI);
        assertThat(savedBot.getBotDifficulty()).isEqualTo(BotDifficulty.MEDIUM);
        assertThat(savedBot.getFirstName()).isEqualTo("Rocco");
        assertThat(savedBot.isEnabled()).isFalse();
        assertThat(matchEntity.getPlayers()).hasSize(2).extracting(UserEntity::getUsername).contains("ABC123-bot-1");
    }

    @Test
    void addBot_onStartedMatch_throwsMatchCompositionLockedException() {
        matchEntity.setStarted(true);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.addBot(host, addBotReqDto(BotDifficulty.EASY, null)))
                .isInstanceOf(MatchCompositionLockedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void addBot_onFullMatch_throwsMatchFullException() {
        List<UserEntity> fullPlayers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fullPlayers.add(new UserEntity("player" + i, "pwd", List.of(UserRole.USER)));
        }
        matchEntity.setPlayers(fullPlayers);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.addBot(host, addBotReqDto(BotDifficulty.EASY, null)))
                .isInstanceOf(MatchFullException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void addBot_hostNotHostingAnOpenMatch_throwsResourceNotFoundException() {
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.addBot(host, addBotReqDto(BotDifficulty.EASY, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeBot_happyPath_removesTheBotAndDeletesItsAccount() {
        UserEntity bot = new UserEntity("ABC123-bot-1", "encoded", List.of(UserRole.AI));
        bot.setId(2L);
        bot.setBotDifficulty(BotDifficulty.HARD);
        matchEntity.getPlayers().add(bot);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        String keyCode = matchService.removeBot(host, "ABC123-bot-1");

        assertThat(keyCode).isEqualTo("ABC123");
        assertThat(matchEntity.getPlayers()).extracting(UserEntity::getUsername).containsExactly("alice");
        verify(userRepository).delete(bot);
        verify(matchRepository).save(matchEntity);
    }

    @Test
    void removeBot_onStartedMatch_throwsMatchCompositionLockedException() {
        matchEntity.setStarted(true);
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.removeBot(host, "ABC123-bot-1"))
                .isInstanceOf(MatchCompositionLockedException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void removeBot_nonExistentOrNonBotUsername_throwsResourceNotFoundException() {
        when(matchRepository.findFirstByHostUserUsernameAndEnded("alice", false)).thenReturn(Optional.of(matchEntity));

        assertThatThrownBy(() -> matchService.removeBot(host, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).delete(any());
    }
}
