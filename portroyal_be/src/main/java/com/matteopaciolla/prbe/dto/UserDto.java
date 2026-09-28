package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "User", description = "User profile returned by the API, including identity and role metadata.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    @Schema(description = "Persistent database identifier of the user.", example = "42")
    private Long id;

    @Schema(description = "Unique username used for login and user resolution.", example = "alice")
    private String username;

    @Schema(description = "Optional first name for the profile.", example = "Alice")
    private String firstName;

    @Schema(description = "Optional last name for the profile.", example = "Rossi")
    private String lastName;

    @Schema(description = "Confirmed or pending email address associated with the account.", example = "alice@example.com")
    private String email;

    @Schema(description = "Telegram identifier when the user is linked to Telegram.", example = "123456789")
    private String telegramId;

    @Schema(description = "Whether the account is currently enabled and usable.", example = "true")
    private Boolean enabled;

    @Schema(description = "Role list granted to the user; typical values are USER, ADMIN or BOT.", example = "[\"USER\", \"ADMIN\"]")
    private List<String> roles;
}