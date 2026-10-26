package com.matteopaciolla.prbe.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matteopaciolla.prbe.annotation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "UserRequest", description = "Payload used to create or update a user profile.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserReqDto {

    @Schema(description = "Account username. Length 5-30, letters, digits, underscore and dash only.", example = "alice42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]{5,30}$", message = "Username should be alphanumeric and between 5 and 30 characters, including -_")
    private String username;

    @Schema(description = "Required by the public web/app registration endpoint. Omit only when registering through the BOT-only Telegram endpoint; the server then generates an internal password that is not returned to the Telegram player. Passwords are limited to 72 UTF-8 bytes.", example = "MyP@ssw0rd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
    private String password;

    @Schema(description = "Optional first name.", example = "Alice")
    @Size(max = 30, message = "First name must be less than 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "First name should contain only letters and spaces")
    private String firstName;

    @Schema(description = "Optional last name.", example = "Rossi")
    @Size(max = 30, message = "Last name must be less than 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Last name should contain only letters and spaces")
    private String lastName;

    @Schema(description = "Email address for account recovery and confirmation flows.", example = "alice@example.com")
    @Email(message = "Email should be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @Schema(description = "Telegram numeric id containing 1-20 digits. Accepted only for BOT-authorized Telegram registration; other accounts link Telegram through the verified account-unification flow.", example = "123456789")
    @Pattern(regexp = "^[0-9]{1,20}$", message = "Telegram ID should contain between 1 and 20 digits")
    private String telegramId;
}