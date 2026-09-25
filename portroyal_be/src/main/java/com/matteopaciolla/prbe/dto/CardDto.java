package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardDto {

    @Schema(example = "101", description = "The card id")
    private Integer id;
    @Schema(example = "Ship", description = "The card type")
    private String type;
    @Schema(example = "⛵\uFE0F\uD83D\uDD34", description = "The card icon")
    private String icon;
    @Schema(example = "6", description = "The card points if it is card that gives points")
    private Integer points;
    @Schema(example = "3", description = "The card cost if it is a card that can be bought")
    private Integer cost;
    @Schema(example = "RED", description = "The card color if it is a Ship, a Merchant or a Clerk")
    private String color;
    @Schema(example = "2", description = "The card money if it is a Ship or an Expedition")
    private Integer money;
    @Schema(example = "1", description = "The card power if it is a Ship, a Pirate or a Sailor")
    private Integer power;
    @Schema(example = "[\"CAPTAIN\", \"CAPTAIN\"]", description = "The list of employees needed for the expedition")
    private List<String> expeditionEmployees;
}
