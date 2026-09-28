package com.matteopaciolla.prbe.dto;

import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(name = "Alert", description = "Notification payload produced by the sentinel subsystem for match lifecycle events.")
@Data
@AllArgsConstructor
public class AlertDto {

    @Schema(description = "Match key code the alert refers to.", example = "ABC123")
    private final String keyCode;

    @Schema(description = "Numeric code identifying the alert type.", example = "1001")
    private final int code;

    @Schema(description = "Additional human-readable description for the alert, if present.", example = "Player joined the match.")
    private final String description;

    @Schema(description = "Standard message generated for the alert.", example = "A player joined the match.")
    private final String message;

    public AlertDto(SentinelAlertMessage sentinelAlertMessage, String keyCode) {
        this.code = sentinelAlertMessage.getCode();
        this.description = null;
        this.message = sentinelAlertMessage.getMessage();
        this.keyCode = keyCode;
    }
}
