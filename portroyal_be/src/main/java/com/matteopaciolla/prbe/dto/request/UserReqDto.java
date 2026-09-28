package com.matteopaciolla.prbe.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "UserRequest", description = "Payload used to create or update a user profile.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserReqDto {

    @Schema(description = "Account username. Length 5-30, letters, digits, underscore and dash only.", example = "alice42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Username is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]{5,30}$", message = "Username should be alphanumeric and between 5 and 30 characters, including -_")
    private String username;

    @Schema(description = "Password for the account. Sent in clear text by the client and then hashed server-side.", example = "MyP@ssw0rd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String email;

    @Schema(description = "Telegram numeric id if the account is linked to a Telegram user.", example = "123456789")
    @Pattern(regexp = "^[0-9]+$", message = "Telegram ID should be a sequence of digits")
    private String telegramId;
}