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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

import static com.matteopaciolla.prbe.util.AuthenticationUtils.getUserName;
import static com.matteopaciolla.prbe.util.AuthenticationUtils.isBotUser;

@Tag(name = "Move", description = "APIs for playing the game")
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

    @Operation(summary = "Insert a move", description = """
Make a move in the game.
If the player is a bot, the _tgId_ header must be set with the username of the player that the bot is playing for.
The move is possible only if the game is in the correct state and the player is the current player.""",
    parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = BH, description = "Bot mandatory header", required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
    },
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New Move request body", required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "{\"move\":\"COMMIT_EXPEDITION\",\"parameterIndex\":0,\"pickPlayerIndex\":-1,\"expeditionEmployeesList\":[\"CAPTAIN\",\"CAPTAIN\",\"SETTLER\"]}")))
    )
    @PostMapping(path = "/move", consumes = "application/json", produces = "application/json")
    public ResponseEntity<MoveResponse> move(
            @RequestHeader(value = BH, required = false) String tgId,
            @Valid @RequestBody MoveReqDto moveReqDto) throws ExecutionException {
        MoveResponse moveResponse;
        if (isBotUser()) {
            if (tgId == null) {
                throw new MandatoryBotParamException();
            }
            moveResponse = gameService.insertMoveWithTelegramId(tgId, moveReqDto);
        } else {
            moveResponse = gameService.insertMoveWithUsername(getUserName(), moveReqDto);
        }
        String keyCode = moveResponse.getData().getMatchKeyCode();
        return ResponseEntity.ok(moveResponse);
    }

    @Operation(summary = "Get a move", description = "Get a single move by match keyCode and move number")
    @GetMapping("/move")
    public ResponseEntity<MoveResponse> getMove(@RequestParam String keyCode, @RequestParam int moveNumber) {
        return ResponseEntity.ok(gameService.getMove(keyCode, moveNumber));
    }

    @Operation(summary = "Get all moves", description = "Get all moves by match keyCode")
    @GetMapping("/moves")
    public ResponseEntity<MovesPageResponse> getMoves(
          @RequestParam String keyCode,
          @RequestParam(defaultValue = "0") Integer pageNumber,
          @RequestParam(defaultValue = "10") Integer pageSize,
          @RequestParam(defaultValue = "TIME_INDEX") MoveRepository.SortField sortField,
          @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {
        return ResponseEntity.ok(gameService.getMoves(keyCode, pageNumber, pageSize, sortField, sortDirection));
    }
}
