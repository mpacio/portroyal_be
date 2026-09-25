package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;


/**
 * This is a representation of a player in the game.
 * {@link com.matteopaciolla.portroyal.core.Player}
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDto {
    private String username;
    private UserDto user;
    private Integer coins;
    private Integer points;
    private Integer power;
    private List<CardDto> employees;
    private List<CardDto> expeditions;
    private Integer contractsCompleted;
    private Integer tradingCapacity;
    private Integer redShipRenounced;
    private Integer blackShipRenounced;
    private List<String> shipColorsRepelled;
    private Boolean taxed;
    private Integer minorSpeculator;
    private Boolean majorSpeculator;
    private Boolean wentBust;
}
