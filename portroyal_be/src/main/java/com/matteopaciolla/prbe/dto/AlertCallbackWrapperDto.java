package com.matteopaciolla.prbe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AlertCallbackWrapperDto {

    private String username;
    private AlertDto alert;
    private String secret;

    public AlertCallbackWrapperDto(String username, AlertDto alert, String secret) {
        this.username = username;
        this.alert = alert;
        this.secret = secret;
    }
}
