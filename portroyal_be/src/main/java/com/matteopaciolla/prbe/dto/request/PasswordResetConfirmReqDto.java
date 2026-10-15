package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "PasswordResetConfirm", description = "Complete a password reset using the one-time code sent by email.")
@Data
public class PasswordResetConfirmReqDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 15, max = 128)
    private String newPassword;
}
