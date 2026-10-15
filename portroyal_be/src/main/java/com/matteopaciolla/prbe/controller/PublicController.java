package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.request.PasswordResetConfirmReqDto;
import com.matteopaciolla.prbe.dto.request.PasswordResetRequestReqDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "Public JSON registration endpoints.")
@Slf4j
@RestController
@RequestMapping(Paths.PUBLIC_PATH)
public class PublicController {

    @Autowired
    private UserService userService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a standard user account. Username, email, and a 15-128 character password are required; first and last names may be omitted and are generated automatically.",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(value = "{\"status\":201,\"message\":\"User registered successfully\",\"data\":{\"id\":42,\"username\":\"alice\",\"email\":\"alice@example.com\",\"enabled\":true,\"roles\":[\"USER\"]}}"))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid user payload")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, false);
        log.info("User {} registered successfully with id {}", savedUserDto.getUsername(), savedUserDto.getId());
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Request a password reset",
            description = "If the email belongs to an enabled, confirmed account, sends a short-lived one-time reset code. The response does not reveal whether the account exists."
    )
    @PostMapping("/password-reset/request")
    public ResponseEntity<VoidResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestReqDto request) {
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(new VoidResponse(
                "If an eligible account exists, password reset instructions will be sent."));
    }

    @Operation(
            summary = "Complete a password reset",
            description = "Changes the password using the one-time code sent to the account email and revokes existing authorizations."
    )
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<VoidResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmReqDto request) {
        userService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new VoidResponse("Password reset successfully."));
    }
}
