package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(name = "UserProfileUpdateRequest", description = "Self-service profile fields. Credentials and identity links use dedicated flows.")
@Data
public class UserProfileUpdateReqDto {

    @Size(max = 30, message = "First name must be less than 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "First name should contain only letters and spaces")
    private String firstName;

    @Size(max = 30, message = "Last name must be less than 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Last name should contain only letters and spaces")
    private String lastName;
}
