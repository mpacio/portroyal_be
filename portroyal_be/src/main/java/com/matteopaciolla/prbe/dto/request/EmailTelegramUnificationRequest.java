package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;
import lombok.Value;

@Schema(name = "EmailTelegramUnificationRequest", description = "Payload used to request OTP for unifying email and Telegram accounts.")
@Value
@NoArgsConstructor
public class EmailTelegramUnificationRequest {
    @NotBlank(message = "email must not be blank")
    @Email(message = "Email format is not valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Schema(description = "The email address of the user which is to be unified with their Telegram account.", example = "user@example.com")
    String email = null;
}
