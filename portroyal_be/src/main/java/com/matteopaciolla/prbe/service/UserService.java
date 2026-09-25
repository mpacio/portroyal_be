package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.converter.UserConverter;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.exceptions.common.UniqueConstraintViolatedException;
import com.matteopaciolla.prbe.exceptions.user.EmailAlreadyConfirmedException;
import com.matteopaciolla.prbe.exceptions.user.TelegramAccountAlreadyUnifiedException;
import com.matteopaciolla.prbe.facade.EmailSenderFacade;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.model.entity.OtpEntity;
import com.matteopaciolla.prbe.repository.OtpRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.matteopaciolla.prbe.util.RandomUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final OtpRepository otpRepository;

    private final EmailSenderFacade emailSenderFacade;

    @Value("${be_app.baseurl}")
    private String APP_BASEURL;

    @Value("${be_app.user.telegram_unification.max_retries:3}")
    private int TELEGRAM_UNIFICATION_MAX_RETRIES;

    /**
     * Registers a new user.
     * For the registration to be successful, the user must not already exist in the system.
     * The userReqDto must contain the username, email, and optionally telegramId.
     * (The telegramId can be null in case of email registration from the web front end).
     *
     * @param userReqDto the user request DTO
     * @param fromBot    true if the request is from a bot, false otherwise
     * @return the registered user DTO
     */
    public UserDto registerUser(UserReqDto userReqDto, boolean fromBot) {
        if (fromBot) {
            requestValidationAgainstBot(userReqDto);
        } else {
            requestValidationAgainstWeb(userReqDto);
        }
        Optional<UserEntity> existingUser = getUser(userReqDto);
        if (existingUser.isPresent()) {
            if (existingUser.get().getUsername().equals(userReqDto.getUsername())) {
                log.error("User with username {} already exists", userReqDto.getUsername());
                throw new UniqueConstraintViolatedException("User with username " + userReqDto.getUsername() + " already exists");
            } else if (existingUser.get().getEmail().equals(userReqDto.getEmail())) {
                log.error("User with email {} already exists", userReqDto.getEmail());
                throw new UniqueConstraintViolatedException("User with email " + userReqDto.getEmail() + " already exists");
            } else if (existingUser.get().getTelegramId().equals(userReqDto.getTelegramId())) {
                log.error("User with telegram id {} already exists", userReqDto.getTelegramId());
                throw new UniqueConstraintViolatedException("User with telegram id " + userReqDto.getTelegramId() + " already exists");
            }
        }
        UserEntity savedUser = createUser(userReqDto, List.of(UserRole.USER));
        UserDto userDto = UserConverter.toDto(savedUser);
        if (!fromBot) {
            sendEmailConfirmationEmail(savedUser);
        }
        return userDto;
    }

    private void requestValidationAgainstWeb(UserReqDto userReqDto) {
        boolean valid = true;
        List<String> missingParams = new ArrayList<>();
        if (userReqDto.getUsername() == null || userReqDto.getUsername().isBlank()) {
            missingParams.add("username");
            valid = false;
        }
        if (userReqDto.getPassword() == null || userReqDto.getPassword().isBlank()) {
            missingParams.add("password");
            valid = false;
        }
        if (userReqDto.getEmail() == null || userReqDto.getEmail().isBlank()) {
            missingParams.add("email");
            valid = false;
        }
        if (!valid) {
            log.error("Missing mandatory parameters from web: {}", missingParams);
            throw new MandatoryParamException("Web registration must provide mandatory parameters", missingParams);
        }
    }

    private void requestValidationAgainstBot(UserReqDto userReqDto) {
        boolean valid = true;
        List<String> missingParams = new ArrayList<>();
        if (userReqDto.getUsername() == null || userReqDto.getUsername().isBlank()) {
            missingParams.add("username");
            valid = false;
        }
        if (userReqDto.getTelegramId() == null || userReqDto.getTelegramId().isBlank()) {
            missingParams.add("telegramId");
            valid = false;
        }
        if (!valid) {
            log.error("Missing mandatory parameters from bot: {}", missingParams);
            throw new MandatoryParamException("Bot registration must provide mandatory parameters", missingParams);
        }
    }

    public UserEntity getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username " + username));
    }

    public UserEntity getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email " + email));
    }

    public UserEntity getUserEntityByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with telegram id " + telegramId));
    }

    public UserDto getUserByUsername(String username) {
        UserEntity user = getUserEntityByUsername(username);
        return UserConverter.toDto(user);
    }

    public UserDto getUserByEmail(String email) {
        UserEntity user = getUserEntityByEmail(email);
        return UserConverter.toDto(user);
    }

    public UserDto getUserByTelegramId(String telegramId) {
        UserEntity user = getUserEntityByTelegramId(telegramId);
        return UserConverter.toDto(user);
    }

    public void deleteUser(String username) {
        UserEntity user = getUserEntityByUsername(username);
        userRepository.delete(user);
    }

    private UserEntity createUser(UserReqDto userReqDto, List<UserRole> roles) {
        UserEntity user = UserConverter.toEntity(userReqDto, roles);
        user.setEnabled(true);
        user.setEmailConfirmed(false);
        String password = userReqDto.getPassword() != null ? userReqDto.getPassword() : getRandomPassword(8);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public UserDto updateUser(UserReqDto userDto) {
        UserEntity user = getExistingUser(userDto);
        boolean modified = false;
        if (userDto.getFirstName() != null && !userDto.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(userDto.getFirstName());
            modified = true;
        }
        if (userDto.getLastName() != null && !userDto.getLastName().equals(user.getLastName())) {
            user.setLastName(userDto.getLastName());
            modified = true;
        }
        if (userDto.getTelegramId() != null && !userDto.getTelegramId().isBlank() && user.getTelegramId() == null) {
            user.setTelegramId(userDto.getTelegramId());
            modified = true;
        }
        if (userDto.getEmail() != null && !userDto.getEmail().isBlank() && user.getEmail() == null) {
            user.setEmail(userDto.getEmail());
            modified = true;
        }
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            modified = true;
        }
        if (modified) {
            user = userRepository.save(user);
            return UserConverter.toDto(user);
        } else {
            return new UserDto();
        }
    }

    private Optional<UserEntity> getUser(UserReqDto userReqDto) {
        return userRepository.findByUsernameOrEmailOrTelegramId(
                userReqDto.getUsername(),
                userReqDto.getEmail() == null ? "" : userReqDto.getEmail(),
                userReqDto.getTelegramId() == null ? "" : userReqDto.getTelegramId()
        );
    }

    private UserEntity getExistingUser(UserReqDto userReqDto) {
        UserEntity user = null;
        if (userReqDto.getUsername() != null) {
            user = userRepository.findByUsername(userReqDto.getUsername()).orElse(null);
            if (user != null && userReqDto.getEmail() != null && user.getEmail() != null && !user.getEmail().equals(userReqDto.getEmail())) {
                log.error("User with username {} has different email {}", userReqDto.getUsername(), userReqDto.getEmail());
                throw new ResourceNotFoundException("User with username " + userReqDto.getUsername() + " has different email " + userReqDto.getEmail());
            }
            if (user != null && userReqDto.getTelegramId() != null && user.getTelegramId() != null && !user.getTelegramId().equals(userReqDto.getTelegramId())) {
                log.error("User with username {} has different telegram id {}", userReqDto.getUsername(), userReqDto.getTelegramId());
                throw new ResourceNotFoundException("User with username " + userReqDto.getUsername() + " has different telegram id " + userReqDto.getTelegramId());
            }
        } else if (userReqDto.getEmail() != null) {
            user = userRepository.findByEmail(userReqDto.getEmail()).orElse(null);
            if (user != null && userReqDto.getTelegramId() != null && user.getTelegramId() != null && !user.getTelegramId().equals(userReqDto.getTelegramId())) {
                log.error("User with email {} has different telegram id {}", userReqDto.getEmail(), userReqDto.getTelegramId());
                throw new ResourceNotFoundException("User with email " + userReqDto.getEmail() + " has different telegram id " + userReqDto.getTelegramId());
            }
        } else if (userReqDto.getTelegramId() != null) {
            user = userRepository.findByTelegramId(userReqDto.getTelegramId()).orElseThrow(() -> new ResourceNotFoundException("User not found with telegram id " + userReqDto.getTelegramId()));
        }
        if (user == null) {
            log.error("User not found with email {}, username {} or telegram id {}", userReqDto.getEmail(), userReqDto.getUsername(), userReqDto.getTelegramId());
            throw new ResourceNotFoundException("User not found. Please provide a valid email and/or username and/or telegram id");
        }
        return user;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        UserEntity user = getUserEntityByUsername(username);
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } else {
            log.error("Invalid old password");
            throw new ResourceNotFoundException("Invalid old password");
        }
    }

    public void sendEmailConfirmationEmail(UserEntity user) {
        String email = user.getEmail();
        Optional<OtpEntity> confirmedEmailAssent = otpRepository.findFirstByEmailAndFlowType(email, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        if (confirmedEmailAssent.isPresent() && confirmedEmailAssent.get().getConfirmedAt() != null) {
            log.debug("Email {} already confirmed", email);
            throw new EmailAlreadyConfirmedException();
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        String token = java.util.UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        OtpEntity emailAssent = new OtpEntity(OtpEntity.FlowType.EMAIL_CONFIRMATION, email, token, expiresAt);
        otpRepository.save(emailAssent);
        // send email
        String subject = "Email Confirmation";
        String htmlBody = "<p>Click <a href=\"https://" + APP_BASEURL + Paths.PUBLIC_PATH + "/confirmEmail?token=" + emailAssent.getToken() + "\">here</a> to confirm your email</p>" +
                "<p>Or copy and paste the following code in the confirmation page: " + emailAssent.getToken() + "</p>";
        String textBody = "Click the following link to confirm your email: https://" + APP_BASEURL + Paths.PUBLIC_PATH + "/confirmEmail?token=" + emailAssent.getToken() +
                " Or copy and paste the following code in the confirmation page: " + emailAssent.getToken();
        log.info("Sending email confirmation to {} with token {}", email, token);
        emailSenderFacade.sendEmail(email, user.getUsername(), subject, htmlBody, textBody);
    }

    public UserDto confirmEmail(String token) {
        OtpEntity emailAssent = otpRepository.findFirstByTokenAndFlowType(token, OtpEntity.FlowType.EMAIL_CONFIRMATION)
                .orElseThrow(() -> new ResourceNotFoundException("Email otp with token " + token + " not found"));
        checkOtpExpiration(emailAssent);
        emailAssent.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(emailAssent);
        String email = emailAssent.getEmail();
        UserEntity user = getUserEntityByEmail(email);
        user.setEmailConfirmed(true);
        userRepository.save(user);
        log.info("Email {} confirmed successfully", email);
        return UserConverter.toDto(user);
    }

    public void sendEmailTelegramUnification(UserEntity user, String telegramId) {
        String email = user.getEmail();
        Optional<OtpEntity> confirmedEmailAssent = otpRepository.findFirstByEmailAndFlowType(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        if (confirmedEmailAssent.isPresent() && confirmedEmailAssent.get().getConfirmedAt() != null) {
            log.debug("Account already unified");
            throw new TelegramAccountAlreadyUnifiedException("Telegram account [id: " + telegramId + "] already unified with email " + email);
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        String token = get4DigitRandomCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        OtpEntity telegramAssent = new OtpEntity(OtpEntity.FlowType.TELEGRAM_UNIFICATION, user.getEmail(), telegramId, token, expiresAt);
        otpRepository.save(telegramAssent);
        // send email
        String subject = "Telegram Account Unification";
        String textBody = "Your code for telegram unification is: " + token + ". Text to the bot: \"/unify " + token + "\"";
        String htmlBody = "<p>Your code for telegram unification is: " + token + ". Text to the bot: <b>/unify " + token + "</b></p>";
        log.info("Sending telegram unification email to {} with token {}", email, token);
        emailSenderFacade.sendEmail(user.getEmail(), user.getUsername(), subject, htmlBody, textBody);
    }

    public UserDto unifyTelegramAndEmailAccounts(String email, String telegramId, String token) {
        OtpEntity otp = otpRepository.findFirstByEmailAndFlowType(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Telegram assent with email " + email + " not found"));
        checkOtpExpiration(otp);
        otp.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(otp);
        userRepository.deleteAllByTelegramId(telegramId);
        UserEntity user = getUserEntityByEmail(email);
        user.setTelegramId(telegramId);
        userRepository.save(user);
        log.info("Telegram account {} unified with email {}", telegramId, email);
        return UserConverter.toDto(user);
    }

    private static void checkOtpExpiration(OtpEntity otp) {
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Email otp with token {} has expired.", otp.getToken());
            throw new ResourceNotFoundException("Email otp with token " + otp.getToken() + " has expired.");
        }
    }
}
