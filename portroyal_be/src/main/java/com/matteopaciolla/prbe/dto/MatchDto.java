package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * This is a representation of a match in the game.
 * {@link com.matteopaciolla.portroyal.core.Match}
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchDto {

    // from lib
    private List<PlayerDto> players;
    private TableDto table;
    private Integer activePlayerIndex;
    private Integer runningPlayerIndex;
    private String currentPhase;
    private Boolean setupDone;
    private Boolean finalTurn;
    private Boolean matchEnded;
    private Integer winnerIndex;
    private Integer firstActivePlayerIndex;
    private Integer lastActivePlayerIndex;
    private CardDto repellingShip;
    private MoveDto lastMove;
    private Integer movesCount;

    //from entity
    private String keyCode;
    private Boolean started;
    private String startedAt;
    private String lastMoveAt;
    private String createdAt;
    private Integer configurationId;
    private List<UserDto> playerUsers;
    private UserDto hostUser;
    private UserDto winnerUser;
    private Boolean ended;
    private String endedAt;
}
