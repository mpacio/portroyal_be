package com.matteopaciolla.prbe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class TgAccountUnificationRequest {

    @Email(message = "Email format is not valid")
    @NotNull(message = "Email is mandatory")
    String email;
    @NotNull(message = "Telegram ID is mandatory")
    String telegramId;
    @NotNull(message = "Token is mandatory")
    String token;
}
