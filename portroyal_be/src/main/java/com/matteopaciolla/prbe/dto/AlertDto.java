package com.matteopaciolla.prbe.dto;

import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlertDto {

    private final String keyCode;
    private final int code;
    private final String description;
    private final String message;

    public AlertDto(SentinelAlertMessage sentinelAlertMessage, String keyCode) {
        this.code = sentinelAlertMessage.getCode();
        this.description = null;
        this.message = sentinelAlertMessage.getMessage();
        this.keyCode = keyCode;
    }
}
