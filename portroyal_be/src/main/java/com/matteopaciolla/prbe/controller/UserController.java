package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.CommonConstants;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.request.ChangePasswordReqDto;
import com.matteopaciolla.prbe.dto.request.EmailTelegramUnificationRequest;
import com.matteopaciolla.prbe.dto.request.TgAccountUnificationRequest;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.exceptions.common.MandatoryBotParamException;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.exceptions.user.EmailNotConfirmedException;
import com.matteopaciolla.prbe.exceptions.user.TelegramAccountAlreadyUnifiedException;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.matteopaciolla.prbe.util.AuthenticationUtils.getUsername;
import static com.matteopaciolla.prbe.util.AuthenticationUtils.isBotUser;

@Tag(name = "User", description = "User profile management, identity queries and account security operations.")
@SecurityRequirements({@SecurityRequirement(name = "oauth2", scopes = {"game:play"})})
@Slf4j
@RestController
@RequestMapping(Paths.USER_PATH)
public class UserController {

    private static final String BH = CommonConstants.BOT_MANDATORY_HEADER;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Retrieve the current user",
            description = "Returns the authenticated user profile resolved from the stable subject in the access token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Current user retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(value = "{\"status\":200,\"message\":\"OK\",\"data\":{\"id\":42,\"username\":\"alice\",\"email\":\"alice@example.com\",\"enabled\":true,\"roles\":[\"USER\"]}}"))),
                    @ApiResponse(responseCode = "401", description = "Authentication required")
            }
    )
    @GetMapping(path = "/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        String username = getUsername();
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
            description = "Resolve a user by username, telegram_id or email. When multiple identifiers are provided, username takes precedence, then telegram_id, then email.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "No identifier provided"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping(path = {"/retrieve", ""})
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "Username to resolve. Highest priority among identifiers.", example = "alice", required = false)
            @RequestParam(value = "username", required = false) String username,
            @Parameter(name = "telegram_id", description = "Telegram id to resolve. Used when username is absent.", example = "123456789", required = false)
            @RequestParam(value = "telegram_id", required = false) String telegramId,
            @Parameter(description = "Email to resolve. Used when username and telegramId are absent.", example = "alice@example.com", required = false)
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
                    @ApiResponse(responseCode = "403", description = "Forbidden for the current principal")
            }
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #username == authentication.tokenAttributes['username']")
    @DeleteMapping(path = "/delete")
    public ResponseEntity<VoidResponse> deleteUser(@Parameter(description = "Username to delete", required = true, example = "alice") @RequestParam String username) {
        userService.deleteUser(username);
        log.info("User account deleted");
        return ResponseEntity.ok(new VoidResponse("User deleted successfully"));
    }

    @Operation(
            summary = "Update a user",
            description = "Partially updates first and last name for the authenticated user or an administrator. Password, email, and Telegram identity changes use dedicated account flows.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #username == authentication.tokenAttributes['username']")
    @PatchMapping(path = "/update")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Username of the user to update", required = true, example = "alice")
            @RequestParam String username,
            @Valid @RequestBody UserReqDto userDto) {
        if (username == null ||username.isBlank()) {
            throw new MandatoryParamException(List.of("username"));
        }
        if (!username.equals(userDto.getUsername())) {
            throw new MandatoryParamException("The username in the path and request body must match");
        }
        UserDto updatedUser = userService.updateUser(userDto);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    @Operation(
            summary = "Change password",
            description = "Changes the password of the currently authenticated user after validating the current password.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password changed successfully"),
                    @ApiResponse(responseCode = "400", description = "New password is empty or invalid")
            }
    )
    @PostMapping(path = "/change-psw")
    public ResponseEntity<VoidResponse> changePassword(@RequestBody ChangePasswordReqDto cpd) {
        if (cpd == null || cpd.getNewPassword() == null || cpd.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new VoidResponse("New password cannot be empty"));
        }
        String username = getUsername();
        userService.changePassword(username,cpd.getOldPassword(), cpd.getNewPassword());
        return ResponseEntity.ok(new VoidResponse("Password changed successfully"));
    }

    @Operation(summary = "Request email confirmation OTP", description = "Sends an email containing the confirmation token required to activate the authenticated user's account.")
    @GetMapping("/new-email-confirmation")
    public ResponseEntity<VoidResponse> requestEmailConfirmation() {
        String username = getUsername();
        UserEntity user = userService.getUserEntityByUsername(username);
        userService.sendEmailConfirmationEmail(user);
        log.info("Email confirmation requested");
        return new ResponseEntity<>(new VoidResponse("Email confirmation sent"), HttpStatus.OK);
    }

    @Operation(
            summary = "Register a telegram player",
            description = "Creates a Telegram-linked user account. This endpoint requires BOT authentication and a tgId header matching the telegramId in the request body. Username and Telegram ID are required; email and password are not required. When omitted, the server assigns an internal random password that is not returned; the Telegram player uses the bot-mediated API and does not sign in with personal password credentials. First and last name are optional and generated when omitted.",
            security = @SecurityRequirement(name = "oauth2", scopes = {"bot:onboard"}),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Telegram user registered successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(value = "{\"status\":201,\"message\":\"User registered successfully by the bot\",\"data\":{\"id\":5,\"username\":\"telegram-user\",\"telegramId\":\"123456789\",\"enabled\":true,\"roles\":[\"USER\"]}}")))
            }
    )
    @PostMapping(path = "/register/telegram")
    public ResponseEntity<UserResponse> registerTelegramPlayer(
            @Parameter(description = "Telegram ID of the player being registered; must match telegramId in the request body.", required = true, example = "123456789")
            @RequestHeader(value = BH, required = true) String tgId,
            @Valid @RequestBody UserReqDto userReqDto) {
        if (tgId == null || tgId.isBlank()) {
            throw new MandatoryBotParamException();
        }
        if (!tgId.equals(userReqDto.getTelegramId())) {
            throw new MandatoryParamException("Telegram ID in header and body must match");
        }
        UserDto savedUserDto = userService.registerUser(userReqDto, true);
        log.info("Telegram player registered successfully");
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully by the bot", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Request Telegram unification OTP",
            description = "Sends a one-time token to the user email so that a Telegram-only account can be merged with an email account.",
            security = {
                    @SecurityRequirement(name = "oauth2", scopes = {"game:play"}),
                    @SecurityRequirement(name = "oauth2", scopes = {"bot:onboard"})
            })
    @PostMapping("/new-tg-unify-email-otp")
    public ResponseEntity<VoidResponse> requestTgUnifyEmailOtp(
            @RequestHeader(value = BH, required = false) String tgId,
            @Valid @RequestBody EmailTelegramUnificationRequest request) {
        String email = request.getEmail();
        String telegramId = null;
        UserEntity user;
        if (isBotUser()) {
            if (tgId == null || tgId.isBlank()) {
                throw new MandatoryBotParamException();
            }
            user = userService.getUserEntityByTelegramId(tgId);
            if (user.getEmail() != null) {
                throw new TelegramAccountAlreadyUnifiedException();
            }
            telegramId = tgId;
        } else {
            if (tgId != null) {
                throw new MandatoryParamException("tgId is accepted only for BOT-authorized account linking");
            }
            String username = getUsername();
            user = userService.getUserEntityByUsername(username);
            if (!user.getEmail().equals(email)) {
                throw new MandatoryParamException("Authenticated user's email does not match the provided email");
            }
            if (user.getTelegramId() != null) {
                throw new TelegramAccountAlreadyUnifiedException();
            }
            if (!user.isEmailConfirmed()) {
                throw new EmailNotConfirmedException();
            }
        }
        userService.sendEmailTelegramUnification(user.getUsername(), email, telegramId);
        log.info("Telegram account unification OTP requested");
        return new ResponseEntity<>(new VoidResponse("Telegram unify email OTP sent"), HttpStatus.OK);
    }

    @Operation(
            summary = "Unify Telegram and email accounts",
            description = "Merges a Telegram-only account with an email-based account through an OTP validation token.",
            security = @SecurityRequirement(name = "oauth2", scopes = {"bot:onboard"}),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Accounts unified successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation or OTP failure")
            }
    )
    @PostMapping(path = "/unify")
    public ResponseEntity<UserResponse> unifyTelegramAndEmailAccounts(
            @RequestHeader(value = BH, required = true) String tgId,
            @Valid @RequestBody TgAccountUnificationRequest request) {
        if (tgId == null || tgId.isBlank()) {
            throw new MandatoryBotParamException();
        }
        UserDto userDto = userService.unifyTelegramAndEmailAccounts(request.getEmail(), tgId, request.getToken());
        return ResponseEntity.ok(new UserResponse("Telegram and email accounts unified successfully", userDto));
    }
}
