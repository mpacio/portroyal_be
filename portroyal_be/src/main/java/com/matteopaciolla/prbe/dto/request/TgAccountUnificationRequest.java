package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Schema(name = "TgAccountUnificationRequest", description = "OTP-backed request to merge a Telegram-only account with an email-based user account.")
@Value
public class TgAccountUnificationRequest {

    @Schema(description = "Email address of the account being unified.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Email format is not valid")
    @NotBlank(message = "Email is mandatory")
    String email;

    @Schema(description = "One-time token sent to the user's email to validate the unification request.", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Token is mandatory")
    String token;
}
