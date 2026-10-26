package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.prbe.annotation.MaxUtf8Bytes;
import com.matteopaciolla.prbe.annotation.MinUnicodeCodePoints;
import com.matteopaciolla.prbe.constants.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(name = "ChangePasswordRequest", description = "Request to replace the password for the currently authenticated user.")
@Data
public class ChangePasswordReqDto {
    @Schema(description = "Current password used to authorize the change.", example = "oldPassword123")
    @NotBlank(message = "Current password is mandatory")
    @MaxUtf8Bytes(value = PasswordPolicy.MAX_UTF8_BYTES, message = "Password must not exceed 72 UTF-8 bytes")
    private String oldPassword;

    @Schema(description = "New password to store after validation. It must contain at least 8 Unicode code points and not exceed 72 UTF-8 bytes; no character classes are required.", example = "NewStrongP@ssw0rd", minLength = 8)
    @NotBlank(message = "New password is mandatory")
    @MinUnicodeCodePoints(value = PasswordPolicy.MIN_CODE_POINTS, message = "Password must contain at least 8 characters")
    @MaxUtf8Bytes(value = PasswordPolicy.MAX_UTF8_BYTES, message = "Password must not exceed 72 UTF-8 bytes")
    private String newPassword;
}
