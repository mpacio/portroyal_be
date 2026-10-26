package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.prbe.annotation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(name = "ChangePasswordRequest", description = "Request to replace the password for the currently authenticated user.")
@Data
public class ChangePasswordReqDto {
    @Schema(description = "Current password used to authorize the change.", example = "oldPassword123")
    @NotBlank(message = "Current password is mandatory")
    @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
    private String oldPassword;

    @Schema(description = "New password to store after validation. It must not exceed 72 UTF-8 bytes.", example = "NewStrongP@ssw0rd")
    @NotBlank(message = "New password is mandatory")
    @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
    private String newPassword;
}
