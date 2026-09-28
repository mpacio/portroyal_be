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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.matteopaciolla.prbe.util.AuthenticationUtils.getUserName;
import static com.matteopaciolla.prbe.util.AuthenticationUtils.isBotUser;

@Tag(name = "Game", description = "Game move execution and move history APIs.")
@SecurityRequirements({@SecurityRequirement(name = "basicAuth")})
@Slf4j
@RestController
@RequestMapping(Paths.GAME_PATH)
public class GameController {

    private static final String BH = CommonConstants.BOT_MANDATORY_HEADER;

    @Autowired
    private GameService gameService;

    @Autowired
    private MatchService matchService;

    @Operation(
            summary = "Insert a move",
            description = "Executes a legal move in the current match. When the caller is a bot, the tgId header identifies the real acting player behind the technical account.",
            parameters = {
                    @Parameter(name = BH, description = "Mandatory only for bot-mediated requests; identifies the real acting player.", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Move payload to execute", required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveReqDto.class),
                            examples = @ExampleObject(value = "{\"move\":\"COMMIT_EXPEDITION\",\"parameterIndex\":0,\"pickPlayerIndex\":-1,\"expeditionEmployeesList\":[\"CAPTAIN\",\"CAPTAIN\",\"SETTLER\"]}"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Move executed successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Move payload invalid or illegal for the current game state"),
                    @ApiResponse(responseCode = "401", description = "Authentication required")
            }
    )
    @PostMapping(path = "/move", consumes = "application/json", produces = "application/json")
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
        return ResponseEntity.ok(moveResponse);
    }

    @Operation(
            summary = "Get a move",
            description = "Returns a single move from a match by keyCode and move number.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Move retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Match or move not found")
            }
    )
    @GetMapping("/move")
    public ResponseEntity<MoveResponse> getMove(
            @Parameter(description = "Unique match key code", example = "ABC123", required = true)
            @RequestParam String keyCode,
            @Parameter(description = "Move number to retrieve", example = "12", required = true)
            @RequestParam int moveNumber) {
        return ResponseEntity.ok(gameService.getMove(keyCode, moveNumber));
    }

    @Operation(
            summary = "Get all moves",
            description = "Returns the paginated move history for a match, with sorting support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Move history retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovesPageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Match not found")
            }
    )
    @GetMapping("/moves")
    public ResponseEntity<MovesPageResponse> getMoves(
          @Parameter(description = "Unique match key code", example = "ABC123", required = true)
          @RequestParam String keyCode,
          @Parameter(description = "Zero-based page number", example = "0", required = false)
          @RequestParam(defaultValue = "0") Integer pageNumber,
          @Parameter(description = "Page size", example = "10", required = false)
          @RequestParam(defaultValue = "10") Integer pageSize,
          @Parameter(description = "Field used to sort move records", example = "TIME_INDEX", required = false)
          @RequestParam(defaultValue = "TIME_INDEX") MoveRepository.SortField sortField,
          @Parameter(description = "Sort direction", example = "ASC", required = false)
          @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {
        return ResponseEntity.ok(gameService.getMoves(keyCode, pageNumber, pageSize, sortField, sortDirection));
    }
}
