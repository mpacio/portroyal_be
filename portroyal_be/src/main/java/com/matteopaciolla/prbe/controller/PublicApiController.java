package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.exceptions.user.EmailNotConfirmedException;
import com.matteopaciolla.prbe.exceptions.user.TelegramAccountAlreadyUnifiedException;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "Public JSON registration and OTP endpoints.")
@Slf4j
@RestController
@RequestMapping(Paths.PUBLIC_PATH)
public class PublicApiController {

    @Autowired
    private UserService userService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a standard user account that can later confirm its email and play matches through the authenticated API.",
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

    @Operation(summary = "Request email confirmation OTP", description = "Sends an email containing the confirmation token required to activate the account.")
    @GetMapping("/newEmailConfirmation")
    public ResponseEntity<VoidResponse> requestEmailConfirmation(
            @Parameter(description = "Email address for which to request confirmation", required = true, example = "alice@example.com")
            @RequestParam String email) {
        UserEntity user = userService.getUserEntityByEmail(email);
        userService.sendEmailConfirmationEmail(user);
        log.info("Email confirmation requested for email {}", email);
        return new ResponseEntity<>(new VoidResponse("Email confirmation sent"), HttpStatus.OK);
    }

    @Operation(summary = "Request Telegram unification OTP", description = "Sends a one-time token to the user email so that a Telegram-only account can be merged with an email account.")
    @GetMapping("/newTgUnifyEmailOtp")
    public ResponseEntity<VoidResponse> requestTgUnifyEmailOtp(
            @Parameter(description = "Email of the account to unify", required = true, example = "alice@example.com")
            @RequestParam String email,
            @Parameter(description = "Optional Telegram id associated with the target account for the unification flow", required = false, example = "123456789")
            @RequestParam(required = false) String telegramId) {
        UserEntity user = userService.getUserEntityByEmail(email);
        if (!user.isEmailConfirmed()) {
            throw new EmailNotConfirmedException();
        }
        if (user.getTelegramId() != null) {
            throw new TelegramAccountAlreadyUnifiedException();
        }
        userService.sendEmailTelegramUnification(user, telegramId);
        log.info("Telegram unify email OTP requested for email {}", email);
        return new ResponseEntity<>(new VoidResponse("Telegram unify email OTP sent"), HttpStatus.OK);
    }
}
