package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
@Schema(description = "Email address requesting an account confirmation message.")
public class EmailConfirmationRequest {

    @Email
    @NotBlank
    @Schema(example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email;
}
