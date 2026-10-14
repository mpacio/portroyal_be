package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;
import lombok.Value;

@Schema(name = "EmailTelegramUnificationRequest", description = "Payload used to request OTP for unifying email and Telegram accounts.")
@Value
@NoArgsConstructor
public class EmailTelegramUnificationRequest {
    @NotBlank(message = "email must not be blank")
    @Schema(description = "The email address of the user which is to be unified with their Telegram account.", example = "user@example.com")
    String email = null;
}
