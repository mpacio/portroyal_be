package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.CommonConstants;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.dto.MatchDto;
import com.matteopaciolla.prbe.dto.MatchInfoDto;
import com.matteopaciolla.prbe.dto.request.AddAiPlayerReqDto;
import com.matteopaciolla.prbe.dto.request.MatchConfigReqDto;
import com.matteopaciolla.prbe.dto.response.*;
import com.matteopaciolla.prbe.exceptions.common.MandatoryBotParamException;
import com.matteopaciolla.prbe.exceptions.match.MultipleHostingDemandException;
import jakarta.validation.Valid;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.service.MatchService;
import com.matteopaciolla.prbe.service.SentinelService;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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


    @Operation(
            summary = "Get matches",
            description = "Lists matches filtered by the player's username and/or ended status, with pagination and sorting support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Matches retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchInfosPageResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required")
            }
    )
    @GetMapping(path = "/retrieve-all", produces = "application/json")
    public ResponseEntity<MatchInfosPageResponse> getAllMatches(
            @Parameter(description = "Filter by ended or active matches. If omitted, all match states are returned.", example = "false", required = false)
            @RequestParam(required = false) Boolean ended,
            @Parameter(description = "Zero-based page number", example = "0", required = false)
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @Parameter(description = "Page size", example = "10", required = false)
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Field used to sort matches", example = "CREATED_AT", required = false)
            @RequestParam(defaultValue = "CREATED_AT") MatchRepository.SortField sortField,
            @Parameter(description = "Sort order direction", example = "DESC", required = false)
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @Parameter(description = "Optional username filter used to retrieve matches for a specific player", example = "alice", required = false)
            @RequestParam(required = false) String username) {
        MatchInfosPageResponse matchDtoList = matchService.getMatchesByPlayer(
                username, ended, pageNumber, pageSize, sortField, sortDirection);
        return ResponseEntity.ok(matchDtoList);
    }

    @Operation(
            summary = "Get a match",
            description = "Returns the full current match snapshot by keyCode. When moveNumber is provided and equals the current move count, the response indicates that no new moves are available.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match retrieved successfully or no new moves available",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Match not found")
            }
    )
    @GetMapping(path = "/retrieve", produces = "application/json")
    public ResponseEntity<MatchResponse> getMatch(
            @Parameter(description = "Unique match key code", example = "ABC123", required = true)
            @RequestParam String keyCode,
            @Parameter(description = "Last move count known by the client. If equal to the current movesCount, no new moves are available.", example = "14", required = false)
            @RequestParam(required = false) Integer moveNumber) {
        MatchDto matchDto = matchService.getMatch(keyCode, isBotUser());
        if (moveNumber != null && moveNumber.equals(matchDto.getMovesCount())) {
            return ResponseEntity.ok(new MatchResponse("No new moves", null));
        }
        return ResponseEntity.ok(new MatchResponse(matchDto));
    }

    @Operation(
            summary = "Join a match",
            description = "Joins an existing non-started match by keyCode. If called by a bot, the tgId header identifies the user represented by the bot.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match joined successfully"),
                    @ApiResponse(responseCode = "400", description = "Match cannot be joined in the current state")
            }
    )
    @PutMapping(path = "/join", produces = "application/json")
    public ResponseEntity<VoidResponse> joinMatch(
            @Parameter(description = "Unique match key code", example = "ABC123", required = true)
            @RequestParam String keyCode,
            @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        matchService.joinMatch(keyCode, user);
        log.info("User {} joined the match with keyCode = {}", user.getUsername(), keyCode);
        sentinelService.sendUpdate(keyCode, user.getUsername(), SentinelAlertMessage.PLAYER_JOINED);
        return ResponseEntity.ok(new VoidResponse("Match joined successfully"));
    }

    @Operation(
            summary = "Host a match",
            description = "Hosts a new match for the authenticated user, optionally using a specific game configuration. For bot users, tgId identifies the real acting user.",
            parameters = {
                    @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional match configuration", required = false,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchConfigReqDto.class),
                            examples = @ExampleObject(value = "{\"id\":1, \"name\":\"default\"}"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match hosted successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchInfoResponse.class))),
                    @ApiResponse(responseCode = "409", description = "A match is already hosted by this user")
            }
    )
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

    @Operation(
            summary = "Close a match",
            description = "Closes the match currently hosted by the user or the match in which the user is participating, if still not started. This does not require the match to be started.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match closed successfully"),
                    @ApiResponse(responseCode = "400", description = "Close operation not allowed in the current match state")
            }
    )
    @PutMapping(path = "/close", produces = "application/json")
    public ResponseEntity<VoidResponse> closeMatch(
            @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        String keyCode = matchService.closeMatch(user);
        log.info("User {} closed the match with keyCode = {}", user.getUsername(), keyCode);
        sentinelService.sendUpdate(keyCode, user.getUsername(), SentinelAlertMessage.MATCH_CLOSED);
        return ResponseEntity.ok(new VoidResponse("Match closed successfully"));
    }

    @Operation(
            summary = "Start a match",
            description = "Starts the match hosted by the authenticated user. Once started, the game becomes active and players can insert moves.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match started successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Match cannot be started in the current state")
            }
    )
    @PutMapping(path = "/start", produces = "application/json")
    public ResponseEntity<MatchResponse> startMatch(
            @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            @RequestHeader(value = BH, required = false) String tgId) {
        UserEntity user = getUserEntity(tgId);
        MatchDto matchDto = matchService.startMatch(user, tgId != null);
        log.info("User {} started the match with keyCode = {}", user.getUsername(), matchDto.getKeyCode());
        sentinelService.sendUpdate(matchDto.getKeyCode(), user.getUsername(), SentinelAlertMessage.MATCH_STARTED);
        return ResponseEntity.ok(new MatchResponse("Match started successfully. Time to insert moves", matchDto));
    }

    @Operation(
            summary = "Add an AI player",
            description = "Adds an autonomous AI player, at the given difficulty, to the not-yet-started match hosted by the authenticated user. The backend plays the AI player's turns automatically once the match starts.",
            parameters = {
                    @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "AI player added successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchInfoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Match is already started, full, or not hosted by the authenticated user")
            }
    )
    @PostMapping(path = "/ai-player", produces = "application/json")
    public ResponseEntity<MatchInfoResponse> addAiPlayer(
            @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            @RequestHeader(value = BH, required = false) String tgId,
            @Valid @RequestBody AddAiPlayerReqDto addAiPlayerReqDto) {
        UserEntity host = getUserEntity(tgId);
        MatchInfoDto matchInfoDto = matchService.addAiPlayer(host, addAiPlayerReqDto);
        log.info("Host {} added an AI player to the match with keyCode = {}", host.getUsername(), matchInfoDto.getKeyCode());
        sentinelService.sendUpdate(matchInfoDto.getKeyCode(), host.getUsername(), SentinelAlertMessage.PLAYER_JOINED);
        return ResponseEntity.ok(new MatchInfoResponse(matchInfoDto));
    }

    @Operation(
            summary = "Remove an AI player",
            description = "Removes a previously added AI player from the not-yet-started match hosted by the authenticated user.",
            parameters = {
                    @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "AI player removed successfully"),
                    @ApiResponse(responseCode = "400", description = "Match is already started, not hosted by the authenticated user, or the given username is not an AI player in this match")
            }
    )
    @DeleteMapping(path = "/ai-player", produces = "application/json")
    public ResponseEntity<VoidResponse> removeAiPlayer(
            @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            @RequestHeader(value = BH, required = false) String tgId,
            @Parameter(description = "Username of the AI player to remove", example = "ABC123-ai-1", required = true)
            @RequestParam String aiPlayerUsername) {
        UserEntity host = getUserEntity(tgId);
        String keyCode = matchService.removeAiPlayer(host, aiPlayerUsername);
        log.info("Host {} removed AI player {} from the match with keyCode = {}", host.getUsername(), aiPlayerUsername, keyCode);
        sentinelService.sendUpdate(keyCode, aiPlayerUsername, SentinelAlertMessage.PLAYER_LEFT);
        return ResponseEntity.ok(new VoidResponse("AI player removed successfully"));
    }

    @Operation(
            summary = "Get match status",
            description = "Returns the currently active match for the authenticated user, or an informational message if no match is being played.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Current match status retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchResponse.class)))
            }
    )
    @GetMapping(path = "/status", produces = "application/json")
    public ResponseEntity<MatchResponse> getMatchStatus(
            @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting user.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
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
