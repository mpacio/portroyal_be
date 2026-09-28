package com.matteopaciolla.prbe.service;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import com.matteopaciolla.portroyal.core.enums.MoveAction;
import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.MoveRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.repository.cachingproxy.MatchRetainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private SentinelService sentinelService;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MoveRepository moveRepository;
    @Mock
    private MatchRetainer matchRetainer;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private MatchEntity matchEntity;
    private UserEntity human;
    private UserEntity bot;

    @BeforeEach
    void setUp() {
        human = new UserEntity("alice", "encoded", List.of(UserRole.USER));
        bot = new UserEntity("ABC123-bot-1", "encoded", List.of(UserRole.AI));
        bot.setBotDifficulty(BotDifficulty.EASY);

        matchEntity = new MatchEntity();
        matchEntity.setKeyCode("ABC123");
        matchEntity.setConfigurationId(1);
        matchEntity.setEnded(false);
    }

    private MoveRecord botMoveRecord(int runningPlayerIndex) {
        MoveRecord moveRecord = new MoveRecord();
        moveRecord.setRunningPlayerIndex(runningPlayerIndex);
        moveRecord.setMove(MoveAction.END_TURN);
        moveRecord.setChoiceIndex(-1);
        moveRecord.setPickPlayerIndex(-1);
        return moveRecord;
    }

    @Test
    void autoPlayBotTurns_stopsImmediatelyOnHumanTurn() {
        matchEntity.setPlayers(List.of(human));
        Match match = mock(Match.class);
        when(match.isMatchEnded()).thenReturn(false);
        when(match.getRunningPlayer()).thenReturn(new Player("alice"));

        gameService.autoPlayBotTurns(match, matchEntity);

        verify(moveRepository, never()).save(any());
        verify(sentinelService, never()).sendUpdate(anyString(), anyString(), any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void autoPlayBotTurns_playsConsecutiveBotTurnsThenStopsOnHumanTurn() {
        matchEntity.setPlayers(List.of(bot, human));
        Match match = mock(Match.class);
        when(match.isMatchEnded()).thenReturn(false);
        when(match.getRunningPlayer()).thenReturn(new Player("ABC123-bot-1"), new Player("ABC123-bot-1"), new Player("alice"));
        when(match.calculateNextMoveRecord(BotDifficulty.EASY)).thenReturn(botMoveRecord(0), botMoveRecord(0));

        gameService.autoPlayBotTurns(match, matchEntity);

        verify(moveRepository, times(2)).save(any());
        verify(sentinelService, times(2)).sendUpdate("ABC123", "ABC123-bot-1", SentinelAlertMessage.MOVES_UPDATED);
        verify(matchRepository, times(1)).save(matchEntity);
        assertThat(matchEntity.getMoves()).hasSize(2);
    }

    @Test
    void autoPlayBotTurns_marksMatchEndedWhenLibraryReportsMatchEnded() {
        matchEntity.setPlayers(List.of(bot));
        Match match = mock(Match.class);
        when(match.isMatchEnded()).thenReturn(false, true);
        when(match.getRunningPlayer()).thenReturn(new Player("ABC123-bot-1"));
        when(match.calculateNextMoveRecord(BotDifficulty.EASY)).thenReturn(botMoveRecord(0));

        gameService.autoPlayBotTurns(match, matchEntity);

        assertThat(matchEntity.getEnded()).isTrue();
        assertThat(matchEntity.getEndedAt()).isNotNull();
        verify(sentinelService).sendUpdate("ABC123", "ABC123-bot-1", SentinelAlertMessage.MATCH_ENDED);
        verify(matchRepository, times(1)).save(matchEntity);
    }

    @Test
    void autoPlayBotTurns_respectsSafetyCapAgainstInfiniteLoop() {
        matchEntity.setPlayers(List.of(bot));
        Match match = mock(Match.class);
        when(match.isMatchEnded()).thenReturn(false);
        when(match.getRunningPlayer()).thenReturn(new Player("ABC123-bot-1"));
        when(match.calculateNextMoveRecord(BotDifficulty.EASY)).thenReturn(botMoveRecord(0));

        gameService.autoPlayBotTurns(match, matchEntity);

        verify(moveRepository, times(500)).save(any());
    }
}
