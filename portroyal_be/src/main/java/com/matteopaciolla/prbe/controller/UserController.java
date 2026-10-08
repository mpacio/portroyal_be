package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.request.ChangePasswordReqDto;
import com.matteopaciolla.prbe.dto.request.TgAccountUnificationRequest;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User profile management, identity queries and account security operations.")
@SecurityRequirements({@SecurityRequirement(name = "basicAuth")})
@Slf4j
@RestController
@RequestMapping(Paths.USER_PATH)
public class UserController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Retrieve the current user",
            description = "Returns the authenticated user profile. The identity is resolved from the HTTP Basic principal currently in the security context.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Current user retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(value = "{\"status\":200,\"message\":\"OK\",\"data\":{\"id\":42,\"username\":\"alice\",\"email\":\"alice@example.com\",\"enabled\":true,\"roles\":[\"USER\"]}}"))),
                    @ApiResponse(responseCode = "401", description = "Authentication required",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping(path = "/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserDto userDto = userService.getUserByUsername(username);
        return ResponseEntity.ok(new UserResponse(userDto));
    }

    /**
     * Retrieves a user by username, telegram id, or email.
     * If multiple identifiers are provided, username takes precedence, followed by telegram id and then email.
     *
     * @param username optional username used to identify the user
     * @param telegramId optional telegram id used to identify the user
     * @param email optional email used to identify the user
     * @return the matching user response
     */
    @Operation(
            summary = "Retrieve a user",
            description = "Resolve a user by username, Telegram ID, or email using the username, telegram_id, or email query parameter. When multiple identifiers are provided, username takes precedence, then telegram_id, then email.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "No identifier provided",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping
    public ResponseEntity<UserResponse> getUser(
            @Parameter(name = "username", description = "Username to resolve. Highest priority among identifiers.", example = "alice", required = false)
            @RequestParam(value = "username", required = false) String username,
            @Parameter(name = "telegram_id", description = "Telegram ID to resolve. Used when username is absent.", example = "123456789", required = false)
            @RequestParam(value = "telegram_id", required = false) String telegramId,
            @Parameter(name = "email", description = "Email to resolve. Used when username and telegram_id are absent.", example = "alice@example.com", required = false)
            @RequestParam(value = "email", required = false) String email) {
        if (username == null && telegramId == null && email == null) {
            throw new MandatoryParamException("At least one of these parameters must be provided", List.of("username", "telegram_id", "email"));
        }

        UserDto userDto;
        if (username != null) {
            userDto = userService.getUserByUsername(username);
        } else if (telegramId != null) {
            userDto = userService.getUserByTelegramId(telegramId);
        } else {
            userDto = userService.getUserByEmail(email);
        }

        return ResponseEntity.ok(new UserResponse(userDto));
    }

    @Operation(
            summary = "Delete a user",
            description = "Deletes the specified user. Admins can delete any user; regular users can only delete their own account.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden for the current principal",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #username == authentication.name")
    @DeleteMapping(path = "/{username}")
    public ResponseEntity<VoidResponse> deleteUser(
            @Parameter(name = "username", description = "Username of the user to delete", required = true, example = "alice", in = ParameterIn.PATH)
            @PathVariable String username) {
        userService.deleteUser(username);
        log.info("User {} deleted successfully", username);
        return ResponseEntity.ok(new VoidResponse("User deleted successfully"));
    }

    @Operation(
            summary = "Partially update a user",
            description = "Partially updates profile fields for the specified user. The username path segment identifies the subject.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PatchMapping(path = "/{username}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(name = "username", description = "Username of the user to update", required = true, example = "alice", in = ParameterIn.PATH)
            @PathVariable String username,
            @Valid @RequestBody UserReqDto userDto) {
        if (!username.equals(userDto.getUsername())) {
            throw new MandatoryParamException("The username in the request body must match the path username", List.of("username"));
        }
        UserDto updatedUser = userService.updateUser(userDto);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    @Operation(
            summary = "Change password",
            description = "Changes the password of the currently authenticated user after validating the current password.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password changed successfully"),
                    @ApiResponse(responseCode = "400", description = "New password is empty or invalid",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PatchMapping(path = "/me/password")
    public ResponseEntity<VoidResponse> changePassword(@RequestBody ChangePasswordReqDto cpd) {
        if (cpd == null || cpd.getNewPassword() == null || cpd.getNewPassword().isBlank()) {
            throw new MandatoryParamException("New password cannot be empty", List.of("newPassword"));
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username,cpd.getOldPassword(), cpd.getNewPassword());
        return ResponseEntity.ok(new VoidResponse("Password changed successfully"));
    }

    @Operation(
            summary = "Register a telegram player",
            description = "Creates a Telegram-linked user account. Username and Telegram ID are required; first name and last name may be omitted and will be generated automatically. This endpoint is intended for bot-only use and requires the bot identity in the security context.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Telegram user registered successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(value = "{\"status\":201,\"message\":\"User registered successfully by the bot\",\"data\":{\"id\":5,\"username\":\"telegram-user\",\"telegramId\":\"123456789\",\"enabled\":true,\"roles\":[\"USER\"]}}")))
            }
    )
    @PostMapping(path = "/telegram-accounts")
    public ResponseEntity<UserResponse> registerTelegramPlayer(@Valid @RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, true);
        log.info("User {} registered successfully by bot with id {}", savedUserDto.getUsername(), savedUserDto.getId());
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully by the bot", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Unify Telegram and email accounts",
            description = "Merges a Telegram-only account with an email-based account through an OTP validation token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Accounts unified successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation or OTP failure",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping(path = "/identity-unifications")
    public ResponseEntity<UserResponse> unifyTelegramAndEmailAccounts(@Valid @RequestBody TgAccountUnificationRequest request) {
        UserDto userDto = userService.unifyTelegramAndEmailAccounts(request.getEmail(), request.getTelegramId(), request.getToken());
        return ResponseEntity.ok(new UserResponse("Telegram and email accounts unified successfully", userDto));
    }
}
