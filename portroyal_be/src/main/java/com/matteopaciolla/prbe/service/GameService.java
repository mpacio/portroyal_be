package com.matteopaciolla.prbe.service;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.enums.MoveAction;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.exceptions.userinput.MoveEndedMatchException;
import com.matteopaciolla.portroyal.exceptions.userinput.UserInputException;
import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.converter.CardConverter;
import com.matteopaciolla.prbe.converter.MoveConverter;
import com.matteopaciolla.prbe.dto.CardDto;
import com.matteopaciolla.prbe.dto.MoveDto;
import com.matteopaciolla.prbe.dto.request.MoveReqDto;
import com.matteopaciolla.prbe.dto.response.*;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.exceptions.game.EndedMatchException;
import com.matteopaciolla.prbe.exceptions.game.MoveExecutionException;
import com.matteopaciolla.prbe.exceptions.game.NotRunningPlayerException;
import com.matteopaciolla.prbe.exceptions.game.NotStartedMatchException;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.MoveEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.repository.cachingproxy.MatchRetainer;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.MoveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    @Value("${portroyal.lib.version}")
    private String prlibVersion;

    @Autowired
    private SentinelService sentinelService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MoveRepository moveRepository;

    @Autowired
    private MatchRetainer matchRetainer;

    @Autowired
    private UserRepository userRepository;

    public MoveResponse insertMoveWithUsername(String username, MoveReqDto moveReqDto) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt")));
        Page<MatchEntity> matches = matchRepository.findByPlayersUsernameAndEnded(username, false, pageable);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("The user with username " + username + " is not in any match");
        } else {
            MatchEntity matchEntity = matches.getContent().getFirst();
            return insertMove(moveReqDto, matchEntity, user);
        }
    }

    public MoveResponse insertMoveWithTelegramId(String telegramId, MoveReqDto moveReqDto) {
        UserEntity user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt")));
        Page<MatchEntity> matches = matchRepository.findByPlayersTelegramIdAndEnded(telegramId, false, pageable);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("The user with telegramId " + telegramId + " is not in any match");
        } else {
            MatchEntity matchEntity = matches.getContent().getFirst();
            return insertMove(moveReqDto, matchEntity, user);
        }
    }

    public MoveResponse insertMove(MoveReqDto moveReqDto, MatchEntity matchEntity, UserEntity user) {
        String username = user.getUsername();
        String keyCode = matchEntity.getKeyCode();
        if (!matchEntity.getPlayers().stream().map(UserEntity::getUsername).toList().contains(username)) {
            throw new ResourceNotFoundException("Player not found in match");
        }
        if (matchEntity.getStarted() == null || !matchEntity.getStarted()) {
            throw new NotStartedMatchException();
        }
        if (matchEntity.getEnded() != null && matchEntity.getEnded()) {
            throw new EndedMatchException();
        }
        Match match = matchRetainer.getMatch(matchEntity);
        if (!match.isRunningPlayer(username)) {
            throw new NotRunningPlayerException(match.getRunningPlayer().getName());
        }
        MoveRecord moveRecord = new MoveRecord();
        moveRecord.setRunningPlayerIndex(match.getRunningPlayerIndex());
        moveRecord.setMove(MoveAction.valueOf(moveReqDto.getMove().toUpperCase(Locale.ROOT)));
        moveRecord.setChoiceIndex(moveReqDto.getParameterIndex() != null ? moveReqDto.getParameterIndex() : -1);
        moveRecord.setPickPlayerIndex(moveReqDto.getPickPlayerIndex() != null ? moveReqDto.getPickPlayerIndex() : -1);
        moveRecord.setExpeditionEmployees(moveReqDto.getExpeditionEmployeesList() != null ?
                moveReqDto.getExpeditionEmployeesList().stream()
                        .map(ExpeditionEmployee::valueOf)
                        .collect(Collectors.toList()) : List.of());
        Card discoveredCard = null;
        try {
            discoveredCard = executeMove(match, moveRecord);
        } catch (UserInputException e) {
            if (e instanceof MoveEndedMatchException) {
                matchEntity.setEnded(true);
                matchEntity.setEndedAt(LocalDateTime.now());
                matchRepository.save(matchEntity);
                sentinelService.sendUpdate(keyCode, user.getUsername(), SentinelAlertMessage.MATCH_ENDED);
                return getMatchEndedMoveResponse();
            } else {
                throw new MoveExecutionException(e);
            }
        }
        MoveRecord addedMove = match.getLastMove();
        MoveEntity moveEntity = MoveConverter.toEntity(addedMove, matchEntity, prlibVersion);
        moveRepository.save(moveEntity);
        matchEntity.addMove(moveEntity);
        matchEntity.setLastMoveAt(LocalDateTime.now());
        matchRepository.save(matchEntity);
        MoveDto moveDto = MoveConverter.toDto(addedMove, match.getCurrentPhase().getClass().getSimpleName());
        sentinelService.sendUpdate(keyCode, user.getUsername(), SentinelAlertMessage.MOVES_UPDATED);
        return new MoveResponse("Move added successfully", moveDto);
    }

    private MoveResponse getMatchEndedMoveResponse() {
        return new MoveResponse("Match ended", null);
    }

    public MoveResponse getMove(String keyCode, int number) {
        Optional<MatchEntity> matchEntity = matchRepository.findByKeyCode(keyCode);
        if (matchEntity.isEmpty()) {
            throw new ResourceNotFoundException("Match not found");
        }
        Long matchId = matchEntity.get().getId();
        MoveDto moveDto = moveRepository.findByMatchIdAndTimeIndex(matchId, number)
                .map(MoveConverter::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Move not found"));
        return new MoveResponse("Move number " + number, moveDto);
    }

    public MovesPageResponse getMoves(String matchKey, Integer pageNumber, Integer pageSize, MoveRepository.SortField sortField, Sort.Direction sortDirection) {
        Long matchId = matchRepository.findByKeyCode(matchKey)
                .map(MatchEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        Sort sort = Sort.by(sortDirection, sortField.getFieldName());
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<MoveEntity> entities = moveRepository.findByMatchId(matchId, pageable);
        List<MoveDto> moveDtos = entities.stream()
                .map(MoveConverter::toDto)
                .toList();
        return new MovesPageResponse(pageNumber, pageSize, entities.getTotalPages(), entities.getTotalElements(), sortField.toString(), sortDirection.toString(), moveDtos);
    }

    private Card executeMove(Match match, MoveRecord moveRecord) throws UserInputException {
        if (moveRecord.getMove() == MoveAction.DISCOVER) {
            return match.discover();
        } else {
            match.executeMove(moveRecord);
            return null;
        }
    }
}
