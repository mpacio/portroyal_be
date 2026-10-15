package com.matteopaciolla.prbe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AlertCallbackWrapperDto {

    private String username;
    private AlertDto alert;

    public AlertCallbackWrapperDto(String username, AlertDto alert) {
        this.username = username;
        this.alert = alert;
    }
}
