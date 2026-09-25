package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.request.ChangePasswordReqDto;
import com.matteopaciolla.prbe.dto.request.TgAccountUnificationRequest;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.response.UserResponse;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "User", description = "User operations")
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

    @Operation(summary = "Retrieve the current user", description = "Retrieve the current user")
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
    @Operation(summary = "Retrieve a user", description = "Retrieve a user by username, telegram id, or email")
    @GetMapping(path = {"/retrieve", ""})
    public ResponseEntity<UserResponse> getUser(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "telegramId", required = false) String telegramId,
            @RequestParam(value = "email", required = false) String email) {
        if (username == null && telegramId == null && email == null) {
            throw new MandatoryParamException("At least one of these parameters must be provided", List.of("username", "telegramId", "email"));
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

    @Operation(summary = "Delete a user", description = "Delete a user by username")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or #username == authentication.name")
    @DeleteMapping(path = "/delete")
    public ResponseEntity<VoidResponse> deleteUser(@RequestParam String username) {
        userService.deleteUser(username);
        log.info("User {} deleted successfully", username);
        return ResponseEntity.ok(new VoidResponse("User deleted successfully"));
    }

    @Operation(summary = "Update a user", description = "Update user data")
    @PutMapping(path = "/update")
    public ResponseEntity<UserResponse> updateUser(
            @RequestParam String username,
            @RequestBody UserReqDto userDto) {
        if (username == null ||username.isBlank()) {
            throw new MandatoryParamException(List.of("username"));
        }
        UserDto updatedUser = userService.updateUser(userDto);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    @Operation(summary = "Change password", description = "Change the password of the current user")
    @PostMapping(path = "/changePsw")
    public ResponseEntity<VoidResponse> changePassword(@RequestBody ChangePasswordReqDto cpd) {
        if (cpd == null || cpd.getNewPassword() == null || cpd.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new VoidResponse("New password cannot be empty"));
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username,cpd.getOldPassword(), cpd.getNewPassword());
        return ResponseEntity.ok(new VoidResponse("Password changed successfully"));
    }

    @Operation(summary = "Register a telegram player", description = """
This endpoint is used to register a telegram player and can be used only by the bot.""")
    @PostMapping(path = "/register/telegram")
    public ResponseEntity<UserResponse> registerTelegramPlayer(@RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, true);
        log.info("User {} registered successfully by bot with id {}", savedUserDto.getUsername(), savedUserDto.getId());
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully by the bot", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(summary = "Unify Telegram and email accounts", description = """
This endpoint is used to unify telegram and email accounts.
This endpoint can be used only by the bot.
If the user is already registered with both telegram and email, the telegram account and email account can be unified.""")
    @PostMapping(path = "/unify")
    public ResponseEntity<UserResponse> unifyTelegramAndEmailAccounts(@Valid @RequestBody TgAccountUnificationRequest request) {
        UserDto userDto = userService.unifyTelegramAndEmailAccounts(request.getEmail(), request.getTelegramId(), request.getToken());
        return ResponseEntity.ok(new UserResponse("Telegram and email accounts unified successfully", userDto));
    }
}
