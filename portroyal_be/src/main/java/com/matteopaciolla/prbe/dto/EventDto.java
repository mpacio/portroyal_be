package com.matteopaciolla.prbe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "Event", description = "Single game event emitted while a move is being processed.")
@Data
public class EventDto {
    @Schema(description = "Machine-readable event code.", example = "DRAW_CARD")
    private String typeCode;

    @Schema(description = "Human-readable description of the event.", example = "A player drew a card from the deck.")
    private String typeDesc;

    @Schema(description = "Username of the player involved in the event, if applicable.", example = "alice")
    private String playerUsername;

    @Schema(description = "List of card ids involved in the event.", example = "[12, 35]")
    private List<Integer> involvedCardIds;

    @Schema(description = "Numeric payload associated with the event, when relevant.", example = "5")
    private Integer value;
}
