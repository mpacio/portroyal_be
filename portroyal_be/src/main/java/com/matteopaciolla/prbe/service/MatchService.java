package com.matteopaciolla.prbe.service;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.prbe.converter.MatchConfigConverter;
import com.matteopaciolla.prbe.converter.MatchConverter;
import com.matteopaciolla.prbe.dto.request.AddAiPlayerReqDto;
import com.matteopaciolla.prbe.dto.request.MatchConfigReqDto;
import com.matteopaciolla.prbe.dto.MatchDto;
import com.matteopaciolla.prbe.dto.MatchInfoDto;
import com.matteopaciolla.prbe.dto.response.MatchInfosPageResponse;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.exceptions.game.NotSinglePlayerMatchException;
import com.matteopaciolla.prbe.exceptions.match.*;
import com.matteopaciolla.prbe.model.entity.AIPlayerEntity;
import com.matteopaciolla.prbe.model.entity.ConfigPropertyEntity;
import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.ConfigPropertyRepository;
import com.matteopaciolla.prbe.repository.cachingproxy.MatchRetainer;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.util.Base36StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MatchService {

    private static final int MAX_PLAYERS = 5;

    private final MatchRepository matchRepository;
    private final ConfigPropertyRepository configPropertyRepository;

    private final MatchRetainer matchRetainer;
    private final GameService gameService;

    public MatchService(MatchRepository matchRepository, ConfigPropertyRepository configPropertyRepository,
                         MatchRetainer matchRetainer, GameService gameService) {
        this.matchRepository = matchRepository;
        this.configPropertyRepository = configPropertyRepository;
        this.matchRetainer = matchRetainer;
        this.gameService = gameService;
    }

    public MatchDto getMatch(String keyCode, boolean fromBot) {
        MatchDto res;
        MatchEntity matchEntity = matchRepository.findByKeyCode(keyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with keyCode " + keyCode));
        if (!matchEntity.getStarted()) {
            res = MatchConverter.toDto(matchEntity, null, fromBot);
        } else {
            Match match = matchRetainer.getMatch(matchEntity);
            res = MatchConverter.toDto(matchEntity, match, fromBot);
        }
        return res;
    }

    public MatchInfosPageResponse getMatchesByPlayer(String username, Boolean ended,
                                                     Integer pageNumber,
                                                     Integer pageSize,
                                                     MatchRepository.SortField sortField,
                                                     Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortDirection, sortField.getFieldName());
        Page<MatchEntity> matches;
        if (username != null) {
            if (ended == null) {
                matches = matchRepository.findByPlayersUsername(username, pageable);
            } else {
                matches = matchRepository.findByPlayersUsernameAndEnded(username, ended, pageable);
            }
        } else {
            if (ended == null) {
                matches = matchRepository.findAll(pageable);
            } else {
                matches = matchRepository.findByEnded(ended, pageable);
            }
        }
        return new MatchInfosPageResponse(pageNumber, pageSize, matches.getTotalPages(), matches.getTotalElements(),
                sortField.getFieldName(), sortDirection.name(), MatchConverter.toListInfoDto(matches.getContent()));
    }

    public MatchDto getHostedMatch(String username, String telegramId) {
        Optional<MatchEntity> firstOpenMatch;
        if (telegramId != null) {
            firstOpenMatch = matchRepository.findFirstByHostUserTelegramIdAndEnded(telegramId, false);
            return firstOpenMatch.map(matchEntity -> MatchConverter.toDto(matchEntity, null,true)).orElse(null);
        } else if (username != null) {
            firstOpenMatch = matchRepository.findFirstByHostUserUsernameAndEnded(username, false);
            return firstOpenMatch.map(matchEntity -> MatchConverter.toDto(matchEntity, null, false)).orElse(null);
        } else {
            throw new MandatoryParamException("username or telegramId must be provided");
        }
    }

    private MatchInfoDto saveNewMatch(UserEntity hostUser, Integer configurationId) {
        String keyCode = Base36StringUtils.shuffleTimestamp();
        MatchEntity matchEntity = new MatchEntity();
        matchEntity.setCreatedAt(LocalDateTime.now());
        matchEntity.setKeyCode(keyCode);
        matchEntity.setConfigurationId(configurationId);
        matchEntity.setHostUser(hostUser);
        matchEntity.setPlayers(new LinkedList<>());
        matchEntity.addPlayer(hostUser);
        MatchEntity savedMatchEntity = matchRepository.save(matchEntity);
        return MatchConverter.toInfoDto(savedMatchEntity);
    }

    public MatchInfoDto hostMatch(UserEntity user, MatchConfigReqDto matchConfigReqDto) {
        if (matchConfigReqDto == null || matchConfigReqDto.getId() == null || matchConfigReqDto.getName() == null) {
            Integer defaultConfigId = 1;
            String defaultConfigName = "default";
            Configuration configuration = Configuration.builder().build();
            matchConfigReqDto = MatchConfigConverter.toDto(configuration, defaultConfigId, defaultConfigName);
            List<ConfigPropertyEntity> configProperties = configPropertyRepository.findByConfigId(defaultConfigId);
            if (configProperties.isEmpty()) {
                MatchConfigConverter.toList(matchConfigReqDto).forEach(configPropertyRepository::save);
            }
        }
        return saveNewMatch(user, matchConfigReqDto.getId());
    }

    public String closeMatch(UserEntity user) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt")));
        Page<MatchEntity> matches = matchRepository.findByPlayersUsernameAndEnded(user.getUsername(), false, pageable);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("No open match found for user " + user.getUsername());
        } else {
            MatchEntity matchEntity = matches.getContent().getFirst();
            matchEntity.setEnded(true);
            matchEntity.setEndedAt(LocalDateTime.now());
            matchRepository.save(matchEntity);
            return matchEntity.getKeyCode();
        }
    }

    public void joinMatch(String keyCode, UserEntity user) {
        String username = user.getUsername();
        // check if the user is already in a match that is not ended
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt")));
        Page<MatchEntity> matches = matchRepository.findByPlayersUsernameAndEnded(username, false, pageable);
        if (!matches.isEmpty()) {
            MatchEntity matchEntity = matches.getContent().getFirst();
            throw new MultipleMatchJoinAttemptException(matchEntity.getKeyCode(), username);
        }
        MatchEntity matchEntity = matchRepository.findByKeyCode(keyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with keyCode " + keyCode));
        // check if the match is already started or ended
        if (matchEntity.getStarted() || matchEntity.getEnded()) {
            throw new JoiningAlreadyStartedMatchException(keyCode);
        }
        // check if the match is already full (counting both human and bot players)
        if (matchEntity.getPlayerCount() >= MAX_PLAYERS) {
            throw new MatchFullException(keyCode);
        }
        // check if the user is already in the match
        if (matchEntity.getPlayers().contains(user)) {
            throw new MultipleMatchJoinAttemptException(keyCode, username);
        }
        matchEntity.getPlayers().add(user);
        matchRepository.save(matchEntity);
    }

    public MatchDto startMatch(UserEntity user, boolean includeTgIds) {
        String username = user.getUsername();
        MatchEntity matchEntity = matchRepository.findFirstByHostUserUsernameAndEnded(username, false)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with username " + username));
        if (matchEntity.getStarted() == null || matchEntity.getStarted()) {
            throw new MultipleMatchStartAttemptException(matchEntity.getKeyCode());
        }
        if (matchEntity.getPlayerCount() < 2) {
            throw new NotSinglePlayerMatchException(matchEntity.getKeyCode());
        }
        matchEntity.setStarted(true);
        matchEntity.setStartedAt(LocalDateTime.now());
        matchRepository.save(matchEntity);
        Match match = matchRetainer.getMatch(matchEntity);
        // the first active player is chosen randomly by the library, so it may already be an AI player:
        // play out any leading AI turns before returning the match state to the caller.
        gameService.autoPlayAiTurns(match, matchEntity);
        return MatchConverter.toDto(matchEntity, match, includeTgIds);
    }

    public MatchInfoDto addAiPlayer(UserEntity host, AddAiPlayerReqDto addAiPlayerReqDto) {
        MatchEntity matchEntity = findHostedOpenMatch(host.getUsername());
        if (Boolean.TRUE.equals(matchEntity.getStarted())) {
            throw new MatchCompositionLockedException(matchEntity.getKeyCode());
        }
        if (matchEntity.getPlayerCount() >= MAX_PLAYERS) {
            throw new MatchFullException(matchEntity.getKeyCode());
        }
        long aiPlayerSlot = matchEntity.getAiPlayers().size() + 1;
        String aiPlayerUsername = matchEntity.getKeyCode() + "-ai-" + aiPlayerSlot;
        String displayName = addAiPlayerReqDto.getName() != null && !addAiPlayerReqDto.getName().isBlank()
                ? addAiPlayerReqDto.getName()
                : "AI Player " + aiPlayerSlot;
        AIPlayerEntity aiPlayer = new AIPlayerEntity(aiPlayerUsername, displayName, addAiPlayerReqDto.getDifficulty());
        matchEntity.addAiPlayer(aiPlayer);
        MatchEntity savedMatchEntity = matchRepository.save(matchEntity);
        log.info("AI player {} (difficulty {}) added to match with keyCode {}", aiPlayerUsername, addAiPlayerReqDto.getDifficulty(), savedMatchEntity.getKeyCode());
        return MatchConverter.toInfoDto(savedMatchEntity);
    }

    public String removeAiPlayer(UserEntity host, String aiPlayerUsername) {
        MatchEntity matchEntity = findHostedOpenMatch(host.getUsername());
        if (Boolean.TRUE.equals(matchEntity.getStarted())) {
            throw new MatchCompositionLockedException(matchEntity.getKeyCode());
        }
        AIPlayerEntity aiPlayer = matchEntity.getAiPlayers().stream()
                .filter(player -> player.getUsername().equals(aiPlayerUsername))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI player with username " + aiPlayerUsername + " not found in match with keyCode " + matchEntity.getKeyCode()));
        matchEntity.getAiPlayers().remove(aiPlayer);
        matchRepository.save(matchEntity);
        log.info("AI player {} removed from match with keyCode {}", aiPlayerUsername, matchEntity.getKeyCode());
        return matchEntity.getKeyCode();
    }

    private MatchEntity findHostedOpenMatch(String hostUsername) {
        return matchRepository.findFirstByHostUserUsernameAndEnded(hostUsername, false)
                .orElseThrow(() -> new ResourceNotFoundException("No open match hosted by user " + hostUsername));
    }

    public Optional<MatchDto> getPlayingMatch(UserEntity user, boolean includeTgIds) {
        String username = user.getUsername();
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt")));
        Page<MatchEntity> matches = matchRepository.findByPlayersUsernameAndEnded(username, false, pageable);
        if (matches.isEmpty()) {
            return Optional.empty();
        } else {
            MatchEntity matchEntity = matches.getContent().getFirst();
            // check if the match is started
            if (matchEntity.getStarted() != null && matchEntity.getStarted()) {
                Match match = matchRetainer.getMatch(matchEntity);
                return Optional.ofNullable(MatchConverter.toDto(matchEntity, match, includeTgIds));
            } else {
                return Optional.ofNullable(MatchConverter.toDto(matchEntity, null, includeTgIds));
            }
        }
    }
}
