package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.CommonConstants;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.dto.MatchDto;
import com.matteopaciolla.prbe.dto.request.MoveReqDto;
import com.matteopaciolla.prbe.dto.response.*;
import com.matteopaciolla.prbe.exceptions.common.MandatoryBotParamException;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.repository.MoveRepository;
import com.matteopaciolla.prbe.service.GameService;
import com.matteopaciolla.prbe.service.MatchService;
import com.matteopaciolla.prbe.service.SentinelService;
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
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.matteopaciolla.prbe.util.AuthenticationUtils.getUserName;
import static com.matteopaciolla.prbe.util.AuthenticationUtils.isBotUser;

@Tag(name = "Moves", description = "Match move creation and move-history resources.")
@SecurityRequirements({@SecurityRequirement(name = "basicAuth")})
@Slf4j
@RestController
public class GameController {

    private static final String BH = CommonConstants.BOT_MANDATORY_HEADER;

    @Autowired
    private GameService gameService;

    @Autowired
    private MatchService matchService;

    @Operation(
            summary = "Create a move",
            description = "Creates and executes a legal move in the authenticated user's current match. When the caller is a bot, the tgId header identifies the real acting player behind the technical account.",
            parameters = {
                    @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting player.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Move payload to execute", required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveReqDto.class),
                            examples = @ExampleObject(value = "{\"move\":\"COMMIT_EXPEDITION\",\"parameterIndex\":0,\"pickPlayerIndex\":-1,\"expeditionEmployeesList\":[\"CAPTAIN\",\"CAPTAIN\",\"SETTLER\"]}"))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Move created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Move payload invalid or illegal for the current game state",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping(path = Paths.MATCH_PATH + "/current/moves", consumes = "application/json", produces = "application/json")
    public ResponseEntity<MoveResponse> move(
            @RequestHeader(value = BH, required = false) String tgId,
            @Valid @RequestBody MoveReqDto moveReqDto) {
        MoveResponse moveResponse;
        if (isBotUser()) {
            if (tgId == null) {
                throw new MandatoryBotParamException();
            }
            moveResponse = gameService.insertMoveWithTelegramId(tgId, moveReqDto);
        } else {
            moveResponse = gameService.insertMoveWithUsername(getUserName(), moveReqDto);
        }
        moveResponse.setStatus(HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(moveResponse);
    }

    @Operation(
            summary = "Get a move",
            description = "Returns a single move from a match by its key and move number.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Move retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Match or move not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping(path = Paths.MATCH_PATH + "/{match-key}/moves/{move-number}")
    public ResponseEntity<MoveResponse> getMove(
            @Parameter(name = "match-key", description = "Unique match key code", example = "ABC123", required = true)
            @PathVariable("match-key") String keyCode,
            @Parameter(name = "move-number", description = "Move number to retrieve", example = "12", required = true)
            @PathVariable("move-number") int moveNumber) {
        return ResponseEntity.ok(gameService.getMove(keyCode, moveNumber));
    }

    @Operation(
            summary = "Get all moves",
            description = "Returns the paginated move history for a match, with sorting support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Move history retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovesPageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Match not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping(path = Paths.MATCH_PATH + "/{match-key}/moves")
    public ResponseEntity<MovesPageResponse> getMoves(
          @Parameter(name = "match-key", description = "Unique match key code", example = "ABC123", required = true)
          @PathVariable("match-key") String keyCode,
          @Parameter(name = "page_number", description = "Zero-based page number", example = "0", required = false)
          @RequestParam(name = "page_number", defaultValue = "0") Integer pageNumber,
          @Parameter(name = "page_size", description = "Page size", example = "10", required = false)
          @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
          @Parameter(name = "sort_field", description = "Field used to sort move records", example = "TIME_INDEX", required = false)
          @RequestParam(name = "sort_field", defaultValue = "TIME_INDEX") MoveRepository.SortField sortField,
          @Parameter(name = "sort_direction", description = "Sort direction", example = "ASC", required = false)
          @RequestParam(name = "sort_direction", defaultValue = "ASC") Sort.Direction sortDirection) {
        return ResponseEntity.ok(gameService.getMoves(keyCode, pageNumber, pageSize, sortField, sortDirection));
    }
}
