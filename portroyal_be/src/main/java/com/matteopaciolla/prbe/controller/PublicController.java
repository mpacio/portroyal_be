package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.EmailConfirmationRequest;
import com.matteopaciolla.prbe.dto.request.TelegramAccountVerificationRequest;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.exceptions.user.EmailNotConfirmedException;
import com.matteopaciolla.prbe.exceptions.user.TelegramAccountAlreadyUnifiedException;
import com.matteopaciolla.prbe.model.entity.UserEntity;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "Public JSON registration and OTP endpoints.")
@Slf4j
@RestController("publicApiController")
public class PublicController {

    @Autowired
    private UserService userService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a standard user account. Username and email are required; password, first name, and last name may be omitted. Missing names are generated automatically.",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(value = "{\"status\":201,\"message\":\"User registered successfully\",\"data\":{\"id\":42,\"username\":\"alice\",\"email\":\"alice@example.com\",\"enabled\":true,\"roles\":[\"USER\"]}}"))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid user payload")
        }
    )
    @PostMapping(Paths.USER_PATH)
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, false);
        log.info("User {} registered successfully with id {}", savedUserDto.getUsername(), savedUserDto.getId());
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Request email confirmation",
            description = "Queues an email containing the confirmation token required to activate the account.",
            responses = @ApiResponse(responseCode = "202", description = "Email confirmation queued"))
    @PostMapping(Paths.EMAIL_CONFIRMATION_PATH)
    public ResponseEntity<VoidResponse> requestEmailConfirmation(
            @Valid @RequestBody EmailConfirmationRequest request) {
        String email = request.getEmail();
        UserEntity user = userService.getUserEntityByEmail(email);
        userService.sendEmailConfirmationEmail(user);
        log.info("Email confirmation requested for email {}", email);
        return new ResponseEntity<>(new VoidResponse(HttpStatus.ACCEPTED.value(), "Email confirmation queued"), HttpStatus.ACCEPTED);
    }

    @Operation(
            summary = "Request Telegram account verification",
            description = "Queues a one-time token to the user email so that a Telegram-only account can be merged with an email account.",
            responses = @ApiResponse(responseCode = "202", description = "Verification email queued"))
    @PostMapping(Paths.TELEGRAM_ACCOUNT_VERIFICATION_PATH)
    public ResponseEntity<VoidResponse> requestTgUnifyEmailOtp(
            @Valid @RequestBody TelegramAccountVerificationRequest request) {
        String email = request.getEmail();
        UserEntity user = userService.getUserEntityByEmail(email);
        if (!user.isEmailConfirmed()) {
            throw new EmailNotConfirmedException();
        }
        if (user.getTelegramId() != null) {
            throw new TelegramAccountAlreadyUnifiedException();
        }
        userService.sendEmailTelegramUnification(user, request.getTelegramId());
        log.info("Telegram unify email OTP requested for email {}", email);
        return new ResponseEntity<>(new VoidResponse(HttpStatus.ACCEPTED.value(), "Telegram account verification queued"), HttpStatus.ACCEPTED);
    }
}
