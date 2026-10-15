package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(name = "PasswordResetRequest", description = "Request a one-time password reset code by email.")
@Data
public class PasswordResetRequestReqDto {

    @Email
    @NotBlank
    private String email;
}
