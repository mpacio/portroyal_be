package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.core.Event;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.exceptions.userinput.UserInputException;
import com.matteopaciolla.prbe.dto.MatchDto;
import com.matteopaciolla.prbe.dto.MatchInfoDto;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;

import java.util.List;
import java.util.stream.Collectors;

import static com.matteopaciolla.prbe.constants.Formats.DATE_TIME_FORMATTER;

public class MatchConverter {

    public static MatchDto toDto(MatchEntity matchEntity, Match match, boolean includeTgId) {
        MatchDto matchDto = new MatchDto();
        boolean notNull = false;
        if (matchEntity != null) {
            matchDto.setKeyCode(matchEntity.getKeyCode());
            matchDto.setStarted(matchEntity.getStarted());
            matchDto.setStartedAt(matchEntity.getStartedAt() != null ? matchEntity.getStartedAt().format(DATE_TIME_FORMATTER) : null);
            matchDto.setLastMoveAt(matchEntity.getLastMoveAt() != null ? matchEntity.getLastMoveAt().format(DATE_TIME_FORMATTER) : null);
            matchDto.setCreatedAt(matchEntity.getCreatedAt() != null ? matchEntity.getCreatedAt().format(DATE_TIME_FORMATTER) : null);
            matchDto.setConfigurationId(matchEntity.getConfigurationId());
            matchDto.setPlayerUsers(matchEntity.getPlayers() != null ? matchEntity.getPlayers().stream().map(userEntity -> UserConverter.toDtoLight(userEntity, includeTgId)).collect(Collectors.toList()) : null);
            matchDto.setHostUser(UserConverter.toDtoLight(matchEntity.getHostUser(), includeTgId));
            matchDto.setWinnerUser(matchEntity.getWinner() != null ? UserConverter.toDto(matchEntity.getWinner()) : null);
            matchDto.setEnded(matchEntity.getEnded());
            matchDto.setEndedAt(matchEntity.getEndedAt() != null ? matchEntity.getEndedAt().format(DATE_TIME_FORMATTER) : null);
            notNull = true;
        }
        if (match != null) {
            matchDto.setPlayers(match.getPlayers().stream().map(PlayerConverter::toDto).collect(Collectors.toList()));
            matchDto.setTable(TableConverter.toDto(match.getTable(), match));
            matchDto.setActivePlayerIndex(match.getActivePlayerIndex());
            matchDto.setRunningPlayerIndex(match.getRunningPlayerIndex());
            matchDto.setCurrentPhase(match.getCurrentPhase().getClass().getSimpleName());
            matchDto.setSetupDone(match.isSetupDone());
            matchDto.setFinalTurn(match.isFinalTurn());
            matchDto.setMatchEnded(match.isMatchEnded());
            try {
                Player winner = match.getWinner();
                matchDto.setWinnerIndex(match.getPlayers().indexOf(winner));
            } catch (UserInputException e) {
                matchDto.setWinnerIndex(null);
            }
            matchDto.setFirstActivePlayerIndex(match.getFirstActivePlayerIndex());
            matchDto.setLastActivePlayerIndex(match.getLastActivePlayerIndex());
            matchDto.setRepellingShip(match.getRepellingShip() != null ? CardConverter.toDto(match.getRepellingShip()) : null);
            matchDto.setMovesCount(match.getMovesCount());
            matchDto.setLastMove(match.getLastMove() != null ? MoveConverter.toDto(match.getLastMove()) : null);
            notNull = true;
        }
        return notNull ? matchDto : null;
    }

    public static MatchInfoDto toInfoDto(MatchEntity match) {
        MatchInfoDto matchInfoDto = new MatchInfoDto();
        matchInfoDto.setKeyCode(match.getKeyCode());
        matchInfoDto.setStarted(match.getStarted());
        matchInfoDto.setStartedAt(match.getStartedAt() != null ? match.getStartedAt().format(DATE_TIME_FORMATTER) : null);
        matchInfoDto.setLastMoveAt(match.getLastMoveAt() != null ? match.getLastMoveAt().format(DATE_TIME_FORMATTER) : null);
        matchInfoDto.setCreatedAt(match.getCreatedAt() != null ? match.getCreatedAt().format(DATE_TIME_FORMATTER) : null);
        matchInfoDto.setConfigurationId(match.getConfigurationId());
        matchInfoDto.setPlayerUsernames(match.getPlayers() != null ? match.getPlayers().stream().map(UserEntity::getUsername).collect(Collectors.toList()) : null);
        matchInfoDto.setHostUsername(match.getHostUser() != null ? match.getHostUser().getUsername() : null);
        matchInfoDto.setWinnerUsername(match.getWinner() != null ? match.getWinner().getUsername() : null);
        matchInfoDto.setEnded(match.getEnded());
        matchInfoDto.setEndedAt(match.getEndedAt() != null ? match.getEndedAt().format(DATE_TIME_FORMATTER) : null);
        matchInfoDto.setMovesCount(match.getMoves() != null ? match.getMoves().size() : null);
        return matchInfoDto;
    }

    public static List<MatchInfoDto> toListInfoDto(List<MatchEntity> matches) {
        return matches.stream().map(MatchConverter::toInfoDto).collect(Collectors.toList());
    }
}
