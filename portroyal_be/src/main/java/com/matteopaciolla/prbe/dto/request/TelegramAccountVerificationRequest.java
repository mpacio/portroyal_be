package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
@Schema(description = "Request an email OTP for linking a Telegram identity to an account.")
public class TelegramAccountVerificationRequest {

    @Email
    @NotBlank
    @Schema(example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email;

    @Schema(example = "123456789")
    String telegramId;
}
