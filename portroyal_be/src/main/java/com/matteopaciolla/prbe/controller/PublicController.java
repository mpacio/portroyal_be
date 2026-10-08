package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.request.EmailConfirmationRequest;
import com.matteopaciolla.prbe.dto.request.TelegramUnificationOtpRequest;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "Public JSON registration and OTP endpoints.")
@Slf4j
@RestController("publicApiController")
@RequestMapping(Paths.PUBLIC_PATH)
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
            @ApiResponse(responseCode = "400", description = "Validation error or invalid user payload",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping("/users")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, false);
        log.info("User {} registered successfully with id {}", savedUserDto.getUsername(), savedUserDto.getId());
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(summary = "Request an email confirmation", description = "Queues an email containing the confirmation token required to activate the account.",
            responses = @ApiResponse(responseCode = "202", description = "Confirmation request accepted",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResponse.class))))
    @PostMapping("/email-confirmations")
    public ResponseEntity<VoidResponse> requestEmailConfirmation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Email confirmation request", required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmailConfirmationRequest.class)))
            @Valid @RequestBody EmailConfirmationRequest request) {
        String email = request.getEmail();
        UserEntity user = userService.getUserEntityByEmail(email);
        userService.sendEmailConfirmationEmail(user);
        log.info("Email confirmation requested for email {}", email);
        return new ResponseEntity<>(new VoidResponse(HttpStatus.ACCEPTED.value(), "Email confirmation request accepted"), HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Request a Telegram unification token", description = "Queues a one-time token email so that a Telegram-only account can be merged with an email account.",
            responses = @ApiResponse(responseCode = "202", description = "Unification request accepted",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResponse.class))))
    @PostMapping("/telegram-unification-requests")
    public ResponseEntity<VoidResponse> requestTgUnifyEmailOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Telegram unification token request", required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelegramUnificationOtpRequest.class)))
            @Valid @RequestBody TelegramUnificationOtpRequest request) {
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
        return new ResponseEntity<>(new VoidResponse(HttpStatus.ACCEPTED.value(), "Telegram unification request accepted"), HttpStatus.ACCEPTED);
    }
}
