package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(name = "TelegramUnificationOtpRequest", description = "Email and optional Telegram ID for an identity-unification OTP request.")
@Data
public class TelegramUnificationOtpRequest {

    @Schema(description = "Email address of the confirmed account to unify.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Email format is not valid")
    @NotNull(message = "Email is mandatory")
    private String email;

    @Schema(description = "Optional Telegram ID associated with the unification request.", example = "123456789")
    @Pattern(regexp = "^[0-9]+$", message = "Telegram ID should be a sequence of digits")
    private String telegramId;
}
