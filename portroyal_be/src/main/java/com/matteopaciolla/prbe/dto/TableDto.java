package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * This is a representation of a table in the game.
 * {@link com.matteopaciolla.portroyal.core.Table}
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableDto {

    private Integer drawPileSize;
    private Integer discardPileSize;
    private List<CardDto> harbor;
    private List<CardDto> expeditions;
    private List<ContractCardDto> contracts;
}
