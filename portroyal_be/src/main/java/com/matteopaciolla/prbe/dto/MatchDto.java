package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * This is a representation of a match in the game.
 * {@link com.matteopaciolla.portroyal.core.Match}
 */
@Schema(name = "Match", description = "Full game-state snapshot for a match, including the board, players and current turn metadata.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchDto {

    @Schema(description = "Players currently participating in the match.")
    private List<PlayerDto> players;

    @Schema(description = "Current table state of the match.")
    private TableDto table;

    @Schema(description = "Index of the player whose turn is currently active. This is not necessarily the player who has to move next (i.e. the running player).", example = "0")
    private Integer activePlayerIndex;

    @Schema(description = "Index of the player that has to move next in the match. This is not necessarily the player who has started the turn (i.e. the active player).", example = "1")
    private Integer runningPlayerIndex;

    @Schema(description = "Current phase name of the game.", example = "TRADE_HIRE_MAIN")
    private String currentPhase;

    @Schema(description = "Whether the initial setup phase has been completed.", example = "true")
    private Boolean setupDone;

    @Schema(description = "Whether the match is in the final turn.", example = "false")
    private Boolean finalTurn;

    @Schema(description = "Whether the match has already ended.", example = "false")
    private Boolean matchEnded;

    @Schema(description = "Winner player index when the match has concluded.", example = "1")
    private Integer winnerIndex;

    @Schema(description = "Index of the first active player of the entire match. This is the player who started the first turn of the match.", example = "0")
    private Integer firstActivePlayerIndex;

    @Schema(description = "Index of the last active player of the entire match. This is the player who ended the last turn of the match.", example = "2")
    private Integer lastActivePlayerIndex;

    @Schema(description = "Current repelling ship card, if any. This ship can be repelled by the current running player.")
    private CardDto repellingShip;

    @Schema(description = "Most recent move executed in the match.")
    private MoveDto lastMove;

    @Schema(description = "Total number of moves already recorded in the match.", example = "27")
    private Integer movesCount;

    @Schema(description = "Unique match code.", example = "ABC123")
    private String keyCode;

    @Schema(description = "Whether the match has been started.", example = "true")
    private Boolean started;

    @Schema(description = "Timestamp when the match started.", example = "2026-09-28T10:30:00Z")
    private String startedAt;

    @Schema(description = "Timestamp of the last recorded move.", example = "2026-09-28T10:35:00Z")
    private String lastMoveAt;

    @Schema(description = "Timestamp when the match was created.", example = "2026-09-28T10:00:00Z")
    private String createdAt;

    @Schema(description = "Configuration id used to initialize the match.", example = "1")
    private Integer configurationId;

    @Schema(description = "User profiles associated with the match players.")
    private List<UserDto> playerUsers;

    @Schema(description = "User profile of the player who created the match (host).")
    private UserDto hostUser;

    @Schema(description = "User profile of the player who won the match, if applicable.")
    private UserDto winnerUser;

    @Schema(description = "Whether the match record is marked as ended in persistence.", example = "false")
    private Boolean ended;// TODO: this may be redundant with matchEnded, but it is used to mark the match as ended in persistence, even if the match is not yet fully completed.

    @Schema(description = "Timestamp when the match ended, if applicable.", example = "2026-09-28T11:00:00Z")
    private String endedAt;
}
