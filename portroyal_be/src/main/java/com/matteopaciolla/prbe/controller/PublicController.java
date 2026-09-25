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
import com.matteopaciolla.prbe.util.StringManipulationUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Slf4j
@Controller
@RequestMapping(Paths.PUBLIC_PATH)
public class PublicController {

    @Autowired
    private UserService userService;

    @RequestMapping("/confirmEmail")
    public ModelAndView confirmEmail(@RequestParam String token, ModelMap model) {
        log.info("Confirming email with token: {}", token);
        UserDto userDto = userService.confirmEmail(token);
        log.info("Email confirmed successfully");
        ResourceBundle bundle = ResourceBundle.getBundle("labels", Locale.ITALY);
        model.addAttribute("title", bundle.getString("email_confirmed_page.title"));
        String message = bundle.getString("email_confirmed_page.message");
        message = StringManipulationUtils.insertVariables(message, Map.of("username", userDto.getUsername()));
        model.addAttribute("message", message);
        return new ModelAndView("email_confirmed_page");
    }

    @Operation(summary = "Register a new user", description = "Register a new user", tags = {"User"})
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserReqDto userReqDto) {
        UserDto savedUserDto = userService.registerUser(userReqDto, false);
        log.info("User {} registered successfully with id {}", savedUserDto.getUsername(), savedUserDto.getId());
        UserResponse res = new UserResponse(HttpStatus.CREATED.value(), "User registered successfully", savedUserDto);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(summary = "Ask for otp email confirmation", description = """
Confirm email with the provided token. The token is sent to the user's email address during registration.
The token is valid for 24 hours.""", tags = {"User"}, hidden = true)
    @GetMapping("/newEmailConfirmation")
    public ResponseEntity<VoidResponse> requestEmailConfirmation(@RequestParam String email) {
        UserEntity user = userService.getUserEntityByEmail(email);
        userService.sendEmailConfirmationEmail(user);
        log.info("Email confirmation requested for email {}", email);
        return new ResponseEntity<>(new VoidResponse("Email confirmation sent"), HttpStatus.OK);
    }

    @Operation(summary = "Request Telegram unification OTP", description = """
Request a one-time password (OTP) for Telegram unification. The OTP is sent to the user's email address.
The OTP is valid for 24 hours.
The user must be registered and have a confirmed email address.""", tags = {"User"}, hidden = true)
    @GetMapping("/newTgUnifyEmailOtp")
    public ResponseEntity<VoidResponse> requestTgUnifyEmailOtp(@RequestParam String email, @RequestParam(required = false) String telegramId) {
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
