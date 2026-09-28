package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 * This is a representation of a player in the game.
 * {@link com.matteopaciolla.portroyal.core.Player}
 */
@Schema(name = "Player", description = "Current snapshot of a player in a match, including resources, score and owned cards.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDto {
    @Schema(description = "Username of the player as resolved in the backend.", example = "alice")
    private String username;

    @Schema(description = "Detailed user profile for this player when available.")
    private UserDto user;

    @Schema(description = "Current coins available to the player.", example = "12")
    private Integer coins;

    @Schema(description = "Current total victory points from completed objectives and cards.", example = "8")
    private Integer points;

    @Schema(description = "Current total power of the player's active board.", example = "7")
    private Integer power;

    @Schema(description = "Employee cards currently hired by the player.")
    private List<CardDto> employees;

    @Schema(description = "Expeditions already committed by the player.")
    private List<CardDto> expeditions;

    @Schema(description = "Number of contracts completed by the player.", example = "2")
    private Integer contractsCompleted;

    @Schema(description = "Current trading capacity of the player. This is the maximum number of cards the player can trade in the current turn.", example = "3")
    private Integer tradingCapacity;

    @Schema(description = "Count of red ships renounced by the player during the match.", example = "1")
    private Integer redShipRenounced;

    @Schema(description = "Count of black ships renounced by the player during the match.", example = "0")
    private Integer blackShipRenounced;

    @Schema(description = "List of ship colors that were repelled by this player.", example = "[\"RED\", \"BLACK\"]")
    private List<String> shipColorsRepelled;

    @Schema(description = "Whether the player has already been taxed in the current state.", example = "false")
    private Boolean taxed;

    @Schema(description = "The number of the times that the player has fulfilled the minor speculator requirements during this match.", example = "1")
    private Integer minorSpeculator;

    @Schema(description = "Whether the player has the major speculator bonus active.", example = "false")
    private Boolean majorSpeculator;

    @Schema(description = "Whether the player has gone bust during the match.", example = "false")
    private Boolean wentBust;
}
