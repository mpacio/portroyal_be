package com.matteopaciolla.prbe.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserReqDto {

    @NotNull(message = "Username is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]{5,30}$", message = "Username should be alphanumeric and between 5 and 30 characters, including -_")
    private String username;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String password;

    @Size(max = 30, message = "First name must be less than 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "First name should contain only letters and spaces")
    private String firstName;

    @Size(max = 30, message = "Last name must be less than 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Last name should contain only letters and spaces")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    @Pattern(regexp = "^[0-9]+$", message = "Telegram ID should be a sequence of digits")
    private String telegramId;
}