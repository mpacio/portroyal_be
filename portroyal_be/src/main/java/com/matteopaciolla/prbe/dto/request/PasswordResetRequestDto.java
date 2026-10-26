package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "PasswordResetRequest", description = "Requests a password reset for a confirmed web/app account.")
@Data
public class PasswordResetRequestDto {

    @Schema(description = "Email address associated with the account.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email format is not valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;
}
