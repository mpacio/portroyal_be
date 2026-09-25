package com.matteopaciolla.prbe.dto;

import lombok.Data;

import java.util.List;

@Data
public class MatchInfoDto {
    private String keyCode;
    private Boolean started;
    private String startedAt;
    private String lastMoveAt;
    private String createdAt;
    private Integer configurationId;
    private List<String> playerUsernames;
    private String hostUsername;
    private String winnerUsername;
    private Boolean ended;
    private String endedAt;
    private Integer movesCount;
}
