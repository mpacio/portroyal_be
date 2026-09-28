package com.matteopaciolla.prbe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "MatchInfo", description = "Compact summary data for a match, suitable for list endpoints and polling.")
@Data
public class MatchInfoDto {
    @Schema(description = "Unique match code used to identify the match.", example = "ABC123")
    private String keyCode;

    @Schema(description = "Whether the match has already started.", example = "true")
    private Boolean started;

    @Schema(description = "Timestamp when the match started.", example = "2026-09-28T10:33:00Z")
    private String startedAt;

    @Schema(description = "Timestamp of the last move executed in the match.", example = "2026-09-28T10:35:00Z")
    private String lastMoveAt;

    @Schema(description = "Timestamp when the match was created.", example = "2026-09-28T10:30:00Z")
    private String createdAt;

    @Schema(description = "Configuration id used to initialize the match.", example = "1")
    private Integer configurationId;

    @Schema(description = "Usernames of all players currently in the match.", example = "[\"alice\", \"bob\"]")
    private List<String> playerUsernames;

    @Schema(description = "Username of the host user who created or owns the match.", example = "alice")
    private String hostUsername;

    @Schema(description = "Username of the winner, if the match is finished.", example = "bob")
    private String winnerUsername;

    @Schema(description = "Whether the match is finished.", example = "false")
    private Boolean ended;

    @Schema(description = "Timestamp when the match ended, if applicable.", example = "2026-09-28T11:00:00Z")
    private String endedAt;

    @Schema(description = "Total number of recorded moves in the match.", example = "27")
    private Integer movesCount;
}
