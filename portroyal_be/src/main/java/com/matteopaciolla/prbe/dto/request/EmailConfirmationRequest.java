package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(name = "EmailConfirmationRequest", description = "Email address for a confirmation token request.")
@Data
public class EmailConfirmationRequest {

    @Schema(description = "Email address to confirm.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Email format is not valid")
    @NotNull(message = "Email is mandatory")
    private String email;
}
