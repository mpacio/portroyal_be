package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * This is a representation of a table in the game.
 * {@link com.matteopaciolla.portroyal.core.Table}
 */
@Schema(name = "Table", description = "Current visible board state of the match: draw stack, discard stack and open offers.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableDto {

    @Schema(description = "Number of cards remaining in the draw pile.", example = "24")
    private Integer drawPileSize;

    @Schema(description = "Number of cards currently in the discard pile.", example = "7")
    private Integer discardPileSize;

    @Schema(description = "Visible cards in the harbor market available to players.")
    private List<CardDto> harbor;

    @Schema(description = "Visible expedition cards exposed on the table.")
    private List<CardDto> expeditions;

    @Schema(description = "Current contract cards available on the table.")
    private List<ContractCardDto> contracts;
}
