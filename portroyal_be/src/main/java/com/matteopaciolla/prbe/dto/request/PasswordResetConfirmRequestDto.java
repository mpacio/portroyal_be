package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.prbe.annotation.MaxUtf8Bytes;
import com.matteopaciolla.prbe.annotation.MinUnicodeCodePoints;
import com.matteopaciolla.prbe.constants.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "PasswordResetConfirmRequest", description = "Confirms a password reset using the emailed single-use token.")
@Data
public class PasswordResetConfirmRequestDto {

    @Schema(description = "Single-use token sent to the account's confirmed email address.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Reset token is mandatory")
    @Size(max = 128, message = "Reset token must not exceed 128 characters")
    private String token;

    @Schema(description = "New password: at least 8 Unicode code points, no required character classes, and at most 72 UTF-8 bytes.", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New password is mandatory")
    @MinUnicodeCodePoints(value = PasswordPolicy.MIN_CODE_POINTS, message = "Password must contain at least 8 characters")
    @MaxUtf8Bytes(value = PasswordPolicy.MAX_UTF8_BYTES, message = "Password must not exceed 72 UTF-8 bytes")
    private String newPassword;
}
