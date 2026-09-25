package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoveDto {

    private Long id;
    private String createdAt;
    private String libVersion;
    private Integer configurationId;
    private String matchKeyCode;
    private Integer timeIndex;
    private String activePlayerUsername;
    private Integer activePlayerIndex;
    private String runningPlayerUsername;
    private Integer runningPlayerIndex;
    private String moveName;
    private Integer parameterIndex;
    private Integer pickPlayerIndex;
    private List<String> expeditionEmployeesList;
    private String notes;
    private EventDto mainEvent;
    private List<EventDto> sideEvents;
    private String currentPhaseName; // Optional, used for contextual information
}
