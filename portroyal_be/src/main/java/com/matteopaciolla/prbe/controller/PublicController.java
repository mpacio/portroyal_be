package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.request.PasswordResetConfirmRequestDto;
import com.matteopaciolla.prbe.dto.request.PasswordResetRequestDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.service.UserService;
import com.matteopaciolla.prbe.service.RateLimitService;
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
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Public", description = "Public JSON registration and account recovery endpoints.")
@Slf4j
@RestController
@RequestMapping(Paths.PUBLIC_PATH)
public class PublicController {

    @Autowired
    private UserService userService;

    @Autowired
    private RateLimitService rateLimitService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a standard web/app account. Username, email, and password are required; first name and last name are optional and generated when omitted. Registration sends an email-confirmation link; the account must be confirmed before password sign-in. Telegram identity linking must use the verified account-unification flow.",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(value = "{\"status\":201,\"message\":\"User registered successfully\",\"data\":{\"id\":42,\"username\":\"alice\",\"email\":\"alice@example.com\",\"enabled\":true,\"roles\":[\"USER\"]}}"))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid user payload"),
            @ApiResponse(responseCode = "413", description = "Request body exceeds 2 MiB"),
            @ApiResponse(responseCode = "429", description = "Registration limit exceeded; see Retry-After")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, false);
        log.info("User registered successfully");
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Request a password reset",
            description = "Returns the same response whether or not the email belongs to an eligible account. A reset token is sent only to enabled accounts with a confirmed email.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Generic reset-request response"),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload"),
                    @ApiResponse(responseCode = "429", description = "Request rate limit exceeded; see Retry-After")
            })
    @PostMapping("/password-reset")
    public ResponseEntity<VoidResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request,
            HttpServletRequest httpRequest) {
        rateLimitService.enforceEmailSend(request.getEmail(), httpRequest.getRemoteAddr());
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(new VoidResponse(
                "If an eligible account exists for that email, password reset instructions have been sent"));
    }

    @Operation(
            summary = "Confirm a password reset",
            description = "Consumes a single-use token sent to the account's confirmed email. Tokens expire after one hour; successful reset revokes existing authorizations.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password reset completed"),
                    @ApiResponse(responseCode = "400", description = "Invalid password"),
                    @ApiResponse(responseCode = "404", description = "Invalid, expired, or already used reset token"),
                    @ApiResponse(responseCode = "429", description = "Request rate limit exceeded; see Retry-After")
            })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<VoidResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequestDto request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new VoidResponse("Password reset completed"));
    }
}
