package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.CommonConstants;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.dto.MatchDto;
import com.matteopaciolla.prbe.dto.MatchInfoDto;
import com.matteopaciolla.prbe.dto.request.MatchConfigReqDto;
import com.matteopaciolla.prbe.dto.response.*;
import com.matteopaciolla.prbe.exceptions.common.MandatoryBotParamException;
import com.matteopaciolla.prbe.exceptions.match.MultipleHostingDemandException;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.service.MatchService;
import com.matteopaciolla.prbe.service.SentinelService;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.matteopaciolla.prbe.util.AuthenticationUtils.getUserName;
import static com.matteopaciolla.prbe.util.AuthenticationUtils.isBotUser;

@Tag(name = "Match", description = "Matches operations")
@SecurityRequirements({@SecurityRequirement(name = "basicAuth")})
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(Paths.MATCH_PATH)
public class MatchController {

    private static final String BH = CommonConstants.BOT_MANDATORY_HEADER;

    private final MatchService matchService;
    private final UserService userService;
    private final SentinelService sentinelService;


    @Operation(summary = "Get matches", description = "Get matches by player username and ended status")
    @GetMapping(path = "/retrieve-all", produces = "application/json")
    public ResponseEntity<MatchInfosPageResponse> getAllMatches(
            @RequestParam(required = false) Boolean ended,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "CREATED_AT") MatchRepository.SortField sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(required = false) String username) {
        MatchInfosPageResponse matchDtoList = matchService.getMatchesByPlayer(
                username, ended, pageNumber, pageSize, sortField, sortDirection);
        return ResponseEntity.ok(matchDtoList);
    }

    @Operation(summary = "Get a match", description = """
Get match by keyCode.
If the _moveNumber_ query parameter is not set, the response will contain the match data in the current state.
If the _moveNumber_ query parameter is set to the last move number, the response will contain a message
saying that there are no new moves. This is useful for polling the match for new moves.""")
    @GetMapping(path = "/retrieve", produces = "application/json")
    public ResponseEntity<MatchResponse> getMatch(@RequestParam String keyCode, @RequestParam(required = false) Integer moveNumber) {
        MatchDto matchDto = matchService.getMatch(keyCode, isBotUser());
        if (moveNumber != null && moveNumber.equals(matchDto.getMovesCount())) {
            return ResponseEntity.ok(new MatchResponse("No new moves", null));
        }
        return ResponseEntity.ok(new MatchResponse(matchDto));
    }

    @Operation(summary = "Join a match", description = """
Join a match by keyCode.
If the player is a bot, the _tgId_ header must be set with the username of the player that the bot is playing for.
The match must be hosted by someone else and not started yet.""")
    @PutMapping(path = "/join", produces = "application/json")
    public ResponseEntity<VoidResponse> joinMatch(
            @RequestParam String keyCode,
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        matchService.joinMatch(keyCode, user);
        log.info("User {} joined the match with keyCode = {}", user.getUsername(), keyCode);
        sentinelService.sendUpdate(keyCode, user.getUsername(), SentinelAlertMessage.PLAYER_JOINED);
        return ResponseEntity.ok(new VoidResponse("Match joined successfully"));
    }

    @Operation(summary = "Host a match", description = """
Host a match.
If the player is a bot, the _tgId_ header must be set with the username of the player that the bot is playing for.
The match must not be hosted by the player yet.
The match will be hosted with the provided configuration, if any. Otherwise, the default configuration will be used.
The match will be hosted with the player as the first player.
The new match keyCode will be returned in the response.""",
    parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = BH, description = "Bot mandatory header", required = false,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"), in = ParameterIn.HEADER)
    },
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Match configuration request body", required = false,
                    content = @io.swagger.v3.oas.annotations.media.Content(examples = @ExampleObject(value = "{\"id\":1, \"name\":\"default\"}"))))
    @PostMapping(path = "/host", produces = "application/json")
    public ResponseEntity<MatchInfoResponse> hostMatch(
            @RequestHeader(value = BH, required = false) String tgId,
            @RequestBody(required = false) MatchConfigReqDto matchConfigReqDto) {
        UserEntity user = getUserEntity(tgId);
        MatchDto matchDto = matchService.getHostedMatch(user.getUsername(), tgId);
        if (matchDto != null) {
            throw new MultipleHostingDemandException(matchDto.getKeyCode(), user.getUsername());
        }
        MatchInfoDto matchInfoDto = matchService.hostMatch(user, matchConfigReqDto);
        log.info("User {} hosted a match with keyCode = {}", user.getUsername(), matchInfoDto.getKeyCode());
        return ResponseEntity.ok(new MatchInfoResponse(matchInfoDto));
    }

    @Operation(summary = "Close a match", description = """
Close a match.
If the player is a bot, the _tgId_ header must be set with the username of the player that the bot is playing for.
The match can be hosted by the someone else but must be not started yet.
The match will be closed and the match will be ended no matter if the match is hosted by the player or not.""")
    @PutMapping(path = "/close", produces = "application/json")
    public ResponseEntity<VoidResponse> closeMatch(
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        String keyCode = matchService.closeMatch(user);
        log.info("User {} closed the match with keyCode = {}", user.getUsername(), keyCode);
        sentinelService.sendUpdate(keyCode, user.getUsername(), SentinelAlertMessage.MATCH_CLOSED);
        return ResponseEntity.ok(new VoidResponse("Match closed successfully"));
    }

    @Operation(summary = "Start a match", description = """
Start a match.
If the player is a bot, the _tgId_ header must be set with the username of the player that the bot is playing for.
The match must be hosted by the player and not started yet.
The match will be started and the players will be able to insert moves.""")
    @PutMapping(path = "/start", produces = "application/json")
    public ResponseEntity<MatchResponse> startMatch(
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        MatchDto matchDto = matchService.startMatch(user, tgId != null);
        log.info("User {} started the match with keyCode = {}", user.getUsername(), matchDto.getKeyCode());
        sentinelService.sendUpdate(matchDto.getKeyCode(), user.getUsername(), SentinelAlertMessage.MATCH_STARTED);
        return ResponseEntity.ok(new MatchResponse("Match started successfully. Time to insert moves", matchDto));
    }

    @Operation(summary = "Get match status", description = """
Get the status of the match being played by the player.
If the player is a bot, the _tgId_ header must be set with the username of the player that the bot is playing for.
The response will contain the match data in the current state.
If the player is not playing any match, the response will contain a message saying that no match is currently being played.""")
    @GetMapping(path = "/status", produces = "application/json")
    public ResponseEntity<MatchResponse> getMatchStatus(
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        Optional<MatchDto> playingMatch = matchService.getPlayingMatch(user, tgId != null);
        if (playingMatch.isEmpty()) {
            String message = String.format("No match is currently being played by %s", user.getUsername());
            return ResponseEntity.ok(new MatchResponse(message, null));
        } else {
            return ResponseEntity.ok(new MatchResponse(playingMatch.get()));
        }
    }

    private UserEntity getUserEntity(String tgId) {
        if (isBotUser()) {
            if (tgId == null) {
                throw new MandatoryBotParamException();
            }
            return userService.getUserEntityByTelegramId(tgId);
        } else {
            return userService.getUserEntityByUsername(getUserName());
        }
    }
}
