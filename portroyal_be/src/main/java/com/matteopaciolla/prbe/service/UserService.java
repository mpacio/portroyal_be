package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.FakeNames;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.matteopaciolla.prbe.util.RandomUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final OtpRepository otpRepository;

    private final EmailSenderFacade emailSenderFacade;

    private final OAuth2AuthorizationRevocationService authorizationRevocationService;

    @Value("${be_app.baseurl}")
    private String APP_BASEURL;

    @Value("${be_app.user.telegram_unification.max_retries:3}")
    private int TELEGRAM_UNIFICATION_MAX_RETRIES;

    /**
     * Registers a new user.
     * For the registration to be successful, the user must not already exist in the system.
     * Web registration requires username, email and password; bot registration requires username and telegramId.
     * Names are optional and will be generated if missing. Email confirmation is sent for web registration.
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
            if (userReqDto.getTelegramId() != null) {
                throw new MandatoryParamException(
                        "Telegram identity must be linked through the authorized account flow");
            }
        }
        Optional<UserEntity> existingUser = getUser(userReqDto);
        if (existingUser.isPresent()) {
            throw new UniqueConstraintViolatedException("Registration could not be completed");
        }
        populateMissingNames(userReqDto);
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
        if (userReqDto.getTelegramId() == null || userReqDto.getTelegramId().isBlank()) {
            missingParams.add("telegramId");
            valid = false;
        }
        if (!valid) {
            log.error("Missing mandatory parameters from bot: {}", missingParams);
            throw new MandatoryParamException("Bot registration must provide mandatory parameters", missingParams);
        }
        if (userReqDto.getEmail() != null || userReqDto.getPassword() != null) {
            throw new MandatoryParamException(
                    "Telegram registration must not provide email or password");
        }
    }

    public UserEntity getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("The requested user could not be found"));
    }

    public UserEntity getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("The requested user could not be found"));
    }

    public UserEntity getUserEntityByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ResourceNotFoundException("The requested user could not be found"));
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

    @Transactional
    public void deleteUser(String username) {
        UserEntity user = getUserEntityByUsername(username);
        authorizationRevocationService.revokePrincipal(user.getUsername());
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

    private void populateMissingNames(UserReqDto userReqDto) {
        if (isMissing(userReqDto.getFirstName()) || isMissing(userReqDto.getLastName())) {
            String fakeFirstName = FakeNames.FIRST_NAMES.get(ThreadLocalRandom.current().nextInt(FakeNames.FIRST_NAMES.size()));
            String fakeLastName = FakeNames.LAST_NAMES.get(ThreadLocalRandom.current().nextInt(FakeNames.LAST_NAMES.size()));
            if (isMissing(userReqDto.getFirstName())) {
                userReqDto.setFirstName(fakeFirstName);
            }
            if (isMissing(userReqDto.getLastName())) {
                userReqDto.setLastName(fakeLastName);
            }
        }
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public UserDto updateUser(UserReqDto userDto) {
        if (userDto.getPassword() != null || userDto.getEmail() != null || userDto.getTelegramId() != null) {
            throw new MandatoryParamException(
                    "Password, email, and Telegram identity changes must use their dedicated account flows");
        }
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
                throw new ResourceNotFoundException("The requested user could not be found");
            }
            if (user != null && userReqDto.getTelegramId() != null && user.getTelegramId() != null && !user.getTelegramId().equals(userReqDto.getTelegramId())) {
                throw new ResourceNotFoundException("The requested user could not be found");
            }
        } else if (userReqDto.getEmail() != null) {
            user = userRepository.findByEmail(userReqDto.getEmail()).orElse(null);
            if (user != null && userReqDto.getTelegramId() != null && user.getTelegramId() != null && !user.getTelegramId().equals(userReqDto.getTelegramId())) {
                throw new ResourceNotFoundException("The requested user could not be found");
            }
        } else if (userReqDto.getTelegramId() != null) {
            user = userRepository.findByTelegramId(userReqDto.getTelegramId())
                    .orElseThrow(() -> new ResourceNotFoundException("The requested user could not be found"));
        }
        if (user == null) {
            throw new ResourceNotFoundException("The requested user could not be found");
        }
        return user;
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        UserEntity user = getUserEntityByUsername(username);
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            authorizationRevocationService.revokePrincipal(user.getUsername());
        } else {
            log.error("Invalid old password");
            throw new ResourceNotFoundException("Invalid old password");
        }
    }

    public void sendEmailConfirmationEmail(UserEntity user) {
        if (user.isEmailConfirmed()) {
            log.debug("Email confirmation is already complete");
            throw new EmailAlreadyConfirmedException();
        }
        String email = user.getEmail();
        Optional<OtpEntity> confirmedEmailAssent = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(email, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        if (confirmedEmailAssent.isPresent() && confirmedEmailAssent.get().getConfirmedAt() != null) {
            log.debug("Email confirmation is already complete");
            throw new EmailAlreadyConfirmedException();
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        String token = java.util.UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        OtpEntity emailAssent = new OtpEntity(OtpEntity.FlowType.EMAIL_CONFIRMATION, email, token, expiresAt);
        otpRepository.save(emailAssent);
        // send email
        String subject = "Email Confirmation";
        String htmlBody = "<p>Click <a href=\"https://" + APP_BASEURL + Paths.PUBLIC_PATH + "/confirm-email?token=" + emailAssent.getToken() + "\">here</a> to confirm your email</p>" +
                "<p>Or copy and paste the following code in the confirmation page: " + emailAssent.getToken() + "</p>";
        String textBody = "Click the following link to confirm your email: https://" + APP_BASEURL + Paths.PUBLIC_PATH + "/confirm-email?token=" + emailAssent.getToken() +
                " Or copy and paste the following code in the confirmation page: " + emailAssent.getToken();
        log.info("Sending email confirmation");
        emailSenderFacade.sendEmail(email, user.getUsername(), subject, htmlBody, textBody);
    }

    public UserDto confirmEmail(String token) {
        OtpEntity otpEntity = otpRepository.findFirstByTokenAndFlowTypeOrderByCreatedAtDesc(token, OtpEntity.FlowType.EMAIL_CONFIRMATION)
                .orElseThrow(() -> new ResourceNotFoundException("Email confirmation could not be completed"));
        checkOtpExpiration(otpEntity);
        otpEntity.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(otpEntity);
        String email = otpEntity.getEmail();
        UserEntity user = getUserEntityByEmail(email);
        user.setEmailConfirmed(true);
        userRepository.save(user);
        log.info("Email confirmation completed");
        return UserConverter.toDto(user);
    }

    public void sendEmailTelegramUnification(String username, String email, String telegramId) {
        Optional<OtpEntity> confirmedEmailAssent = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        if (confirmedEmailAssent.isPresent() && confirmedEmailAssent.get().getConfirmedAt() != null) {
            log.debug("Account already unified");
            throw new TelegramAccountAlreadyUnifiedException();
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        String token = get4DigitRandomCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        OtpEntity telegramAssent = new OtpEntity(OtpEntity.FlowType.TELEGRAM_UNIFICATION, email, telegramId, token, expiresAt);
        otpRepository.save(telegramAssent);
        // send email
        String subject = "Telegram Account Unification";
        String textBody = "Your code for telegram unification is: " + token + ". Text to the bot: \"/unify " + token + "\"";
        String htmlBody = "<p>Your code for telegram unification is: " + token + ". Text to the bot: <b>/unify " + token + "</b></p>";
        log.info("Sending Telegram account unification email");
        emailSenderFacade.sendEmail(email, username, subject, htmlBody, textBody);
    }

    @Transactional
    public UserDto unifyTelegramAndEmailAccounts(String email, String telegramId, String token) {
        OtpEntity otpEntity = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Telegram account unification could not be completed"));
        if (!otpEntity.getToken().equals(token)) {
            throw new ResourceNotFoundException("Telegram account unification could not be completed");
        }
        checkOtpExpiration(otpEntity);
        otpEntity.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(otpEntity);
        userRepository.findByTelegramId(telegramId)
                .filter(telegramUser -> telegramUser.getEmail() == null
                        || !telegramUser.getEmail().equalsIgnoreCase(email))
                .ifPresent(telegramUser -> authorizationRevocationService.revokePrincipal(telegramUser.getUsername()));
        userRepository.deleteAllByTelegramId(telegramId);
        UserEntity user = getUserEntityByEmail(email);
        user.setTelegramId(telegramId);
        userRepository.save(user);
        log.info("Telegram account unification completed");
        return UserConverter.toDto(user);
    }

    private static void checkOtpExpiration(OtpEntity otpEntity) {
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("The one-time token has expired");
        }
    }
}
