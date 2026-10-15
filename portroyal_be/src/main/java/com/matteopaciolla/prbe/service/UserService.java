package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.FakeNames;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.converter.UserConverter;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.dto.request.UserProfileUpdateReqDto;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
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
import com.matteopaciolla.prbe.security.OAuthAuthorizationRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_RESET_MAX_RETRIES = 5;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final OtpRepository otpRepository;

    private final EmailSenderFacade emailSenderFacade;
    private final OAuthAuthorizationRevocationService authorizationRevocationService;

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
        }
        Optional<UserEntity> existingUser = getUser(userReqDto);
        if (existingUser.isPresent()) {
            if (Objects.equals(existingUser.get().getUsername(), userReqDto.getUsername())) {
                log.error("User with username {} already exists", userReqDto.getUsername());
                throw new UniqueConstraintViolatedException("User with username " + userReqDto.getUsername() + " already exists");
            } else if (userReqDto.getEmail() != null && Objects.equals(existingUser.get().getEmail(), userReqDto.getEmail())) {
                log.error("User with email {} already exists", userReqDto.getEmail());
                throw new UniqueConstraintViolatedException("User with email " + userReqDto.getEmail() + " already exists");
            } else if (userReqDto.getTelegramId() != null && Objects.equals(existingUser.get().getTelegramId(), userReqDto.getTelegramId())) {
                log.error("User with telegram id {} already exists", userReqDto.getTelegramId());
                throw new UniqueConstraintViolatedException("User with telegram id " + userReqDto.getTelegramId() + " already exists");
            }
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
        authorizationRevocationService.revokeUserAuthorizations(username);
        userRepository.delete(user);
    }

    private UserEntity createUser(UserReqDto userReqDto, List<UserRole> roles) {
        UserEntity user = UserConverter.toEntity(userReqDto, roles);
        user.setEnabled(true);
        user.setEmailConfirmed(false);
        if (userReqDto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userReqDto.getPassword()));
        }
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

    public UserDto updateUserProfile(String username, UserProfileUpdateReqDto profile) {
        UserEntity user = getUserEntityByUsername(username);
        boolean modified = false;
        if (profile.getFirstName() != null && !profile.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(profile.getFirstName());
            modified = true;
        }
        if (profile.getLastName() != null && !profile.getLastName().equals(user.getLastName())) {
            user.setLastName(profile.getLastName());
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

    public void changePassword(String username, String oldPassword, String newPassword) {
        UserEntity user = getUserEntityByUsername(username);
        if (oldPassword != null && user.getPassword() != null
                && passwordEncoder.matches(oldPassword, user.getPassword())
                && newPassword != null && newPassword.length() >= 15 && newPassword.length() <= 128) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            authorizationRevocationService.revokeUserAuthorizations(username);
        } else {
            throw new BaseClientCausedException(
                    "The current password is invalid or the new password does not meet policy.");
        }
    }

    public void sendEmailConfirmationEmail(UserEntity user) {
        if (user.isEmailConfirmed()) {
            log.debug("Email {} already confirmed", user.getEmail());
            throw new EmailAlreadyConfirmedException();
        }
        String email = user.getEmail();
        Optional<OtpEntity> confirmedEmailAssent = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(email, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        if (confirmedEmailAssent.isPresent() && confirmedEmailAssent.get().getConfirmedAt() != null) {
            log.debug("Email {} already confirmed", email);
            throw new EmailAlreadyConfirmedException();
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        OtpEntity emailAssent = new OtpEntity(
                OtpEntity.FlowType.EMAIL_CONFIRMATION, email, hashToken(token), expiresAt);
        otpRepository.save(emailAssent);
        // send email
        String subject = "Email Confirmation";
        String htmlBody = "<p>Click <a href=\"https://" + APP_BASEURL + Paths.PUBLIC_PATH + "/confirm-email?token=" + token + "\">here</a> to confirm your email</p>" +
                "<p>Or copy and paste the following code in the confirmation page: " + token + "</p>";
        String textBody = "Click the following link to confirm your email: https://" + APP_BASEURL + Paths.PUBLIC_PATH + "/confirm-email?token=" + token +
                " Or copy and paste the following code in the confirmation page: " + token;
        log.info("Sending email confirmation to {}", email);
        emailSenderFacade.sendEmail(email, user.getUsername(), subject, htmlBody, textBody);
    }

    public UserDto confirmEmail(String token) {
        OtpEntity otpEntity = findOtpByToken(token, OtpEntity.FlowType.EMAIL_CONFIRMATION);
        checkOtpExpiration(otpEntity);
        upgradeLegacyOtpToken(otpEntity, token);
        otpEntity.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(otpEntity);
        String email = otpEntity.getEmail();
        UserEntity user = getUserEntityByEmail(email);
        user.setEmailConfirmed(true);
        userRepository.save(user);
        log.info("Email {} confirmed successfully", email);
        return UserConverter.toDto(user);
    }

    public void sendEmailTelegramUnification(String username, String email, String telegramId) {
        Optional<OtpEntity> confirmedEmailAssent = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        if (confirmedEmailAssent.isPresent() && confirmedEmailAssent.get().getConfirmedAt() != null) {
            log.debug("Account already unified");
            throw new TelegramAccountAlreadyUnifiedException("Telegram account [id: " + telegramId + "] already unified with email " + email);
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        String token = generateUnificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        OtpEntity telegramAssent = new OtpEntity(
                OtpEntity.FlowType.TELEGRAM_UNIFICATION, email, telegramId, hashToken(token), expiresAt);
        otpRepository.save(telegramAssent);
        // send email
        String subject = "Telegram Account Unification";
        String textBody = "Your code for telegram unification is: " + token + ". Text to the bot: \"/unify " + token + "\"";
        String htmlBody = "<p>Your code for telegram unification is: " + token + ". Text to the bot: <b>/unify " + token + "</b></p>";
        log.info("Sending Telegram unification email to {}", email);
        emailSenderFacade.sendEmail(email, username, subject, htmlBody, textBody);
    }

    public UserDto unifyTelegramAndEmailAccounts(String email, String telegramId, String token) {
        OtpEntity otpEntity = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(email, OtpEntity.FlowType.TELEGRAM_UNIFICATION)
                .orElseThrow(UserService::invalidOtp);
        if (failedAttempts(otpEntity) >= TELEGRAM_UNIFICATION_MAX_RETRIES) {
            throw invalidOtp();
        }
        if (!tokenMatches(token, otpEntity.getToken())) {
            otpEntity.setFailedAttempts(failedAttempts(otpEntity) + 1);
            otpRepository.save(otpEntity);
            throw invalidOtp();
        }
        if (otpEntity.getTelegramId() != null && !otpEntity.getTelegramId().equals(telegramId)) {
            throw invalidOtp();
        }
        checkOtpExpiration(otpEntity);
        upgradeLegacyOtpToken(otpEntity, token);
        otpEntity.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(otpEntity);
        userRepository.deleteAllByTelegramId(telegramId);
        UserEntity user = getUserEntityByEmail(email);
        user.setTelegramId(telegramId);
        userRepository.save(user);
        log.info("Telegram account {} unified with email {}", telegramId, email);
        return UserConverter.toDto(user);
    }

    public void requestPasswordReset(String email) {
        Optional<UserEntity> userOptional = userRepository.findByEmailAndEnabled(email, true);
        if (userOptional.isEmpty() || !userOptional.get().isEmailConfirmed()) {
            return;
        }

        Optional<OtpEntity> latestReset = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(
                email, OtpEntity.FlowType.PASSWORD_RESET);
        if (latestReset.isPresent()
                && latestReset.get().getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            return;
        }
        otpRepository.deleteAllByEmailAndFlowType(email, OtpEntity.FlowType.PASSWORD_RESET);

        String token = generateSecureToken();
        OtpEntity reset = new OtpEntity(OtpEntity.FlowType.PASSWORD_RESET, email, hashToken(token),
                LocalDateTime.now().plusMinutes(15));
        otpRepository.save(reset);
        String subject = "Password reset";
        String textBody = "Use this one-time code to reset your Port Royal password: " + token
                + ". It expires in 15 minutes.";
        String htmlBody = "<p>Use this one-time code to reset your Port Royal password:</p><p><b>"
                + token + "</b></p><p>It expires in 15 minutes.</p>";
        emailSenderFacade.sendEmail(email, userOptional.get().getUsername(), subject, htmlBody, textBody);
    }

    public void resetPassword(String email, String token, String newPassword) {
        OtpEntity reset = otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(
                        email, OtpEntity.FlowType.PASSWORD_RESET)
                .orElseThrow(UserService::invalidOtp);
        if (failedAttempts(reset) >= PASSWORD_RESET_MAX_RETRIES) {
            throw invalidOtp();
        }
        if (!tokenMatches(token, reset.getToken())) {
            reset.setFailedAttempts(failedAttempts(reset) + 1);
            otpRepository.save(reset);
            throw invalidOtp();
        }
        checkOtpExpiration(reset);
        if (newPassword == null || newPassword.length() < 15 || newPassword.length() > 128) {
            throw new MandatoryParamException("Password must be between 15 and 128 characters");
        }
        UserEntity user = userRepository.findByEmailAndEnabled(email, true)
                .filter(UserEntity::isEmailConfirmed)
                .orElseThrow(UserService::invalidOtp);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        reset.setConfirmedAt(LocalDateTime.now());
        otpRepository.save(reset);
        authorizationRevocationService.revokeUserAuthorizations(user.getUsername());
    }

    private OtpEntity findOtpByToken(String token, OtpEntity.FlowType flowType) {
        return otpRepository.findFirstByTokenAndFlowTypeOrderByCreatedAtDesc(hashToken(token), flowType)
                .or(() -> otpRepository.findFirstByTokenAndFlowTypeOrderByCreatedAtDesc(token, flowType))
                .orElseThrow(UserService::invalidOtp);
    }

    private static void upgradeLegacyOtpToken(OtpEntity otpEntity, String token) {
        if (!isHashedToken(otpEntity.getToken())) {
            otpEntity.setToken(hashToken(token));
        }
    }

    private static boolean tokenMatches(String presentedToken, String persistedToken) {
        if (presentedToken == null || persistedToken == null) {
            return false;
        }
        String comparisonValue = isHashedToken(persistedToken) ? hashToken(presentedToken) : presentedToken;
        return MessageDigest.isEqual(
                comparisonValue.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                persistedToken.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static boolean isHashedToken(String token) {
        return token != null && token.matches("[0-9a-f]{64}");
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String generateUnificationCode() {
        return String.format("%08d", SECURE_RANDOM.nextInt(100_000_000));
    }

    private static String hashToken(String token) {
        if (token == null) {
            return "";
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static ResourceNotFoundException invalidOtp() {
        return new ResourceNotFoundException("The one-time code is invalid or expired.");
    }

    private static int failedAttempts(OtpEntity otpEntity) {
        return otpEntity.getFailedAttempts() == null ? 0 : otpEntity.getFailedAttempts();
    }

    private static void checkOtpExpiration(OtpEntity otpEntity) {
        if (otpEntity.getExpiresAt() == null || otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalidOtp();
        }
    }
}
