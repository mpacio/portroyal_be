package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "ChangePasswordRequest", description = "Request to replace the password for the currently authenticated user.")
@Data
public class ChangePasswordReqDto {
    @Schema(description = "Current password used to authorize the change.", example = "oldPassword123")
    @NotBlank
    private String oldPassword;

    @Schema(description = "New password to store after validation.", example = "NewStrongP@ssw0rd")
    @NotBlank
    @Size(min = 15, max = 128, message = "Password must be between 15 and 128 characters")
    private String newPassword;
}
