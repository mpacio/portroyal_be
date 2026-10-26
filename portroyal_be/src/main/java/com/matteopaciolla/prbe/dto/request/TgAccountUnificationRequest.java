package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;
import lombok.Value;

@Schema(name = "TgAccountUnificationRequest", description = "OTP-backed request to merge a Telegram-only account with an email-based user account.")
@Value
@NoArgsConstructor
public class TgAccountUnificationRequest {

    @Schema(description = "Email address of the account being unified.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Email format is not valid")
    @NotBlank(message = "Email is mandatory")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    String email = null;

    @Schema(description = "One-time token sent to the user's email to validate the unification request.", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Token is mandatory")
    @Pattern(regexp = "^[0-9]{4}$", message = "Token must contain exactly 4 digits")
    String token = null;
}
