package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.FakeNames;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.exceptions.common.UniqueConstraintViolatedException;
import com.matteopaciolla.prbe.facade.EmailSenderFacade;
import com.matteopaciolla.prbe.model.entity.OtpEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.OtpRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.EmailRepository;
import com.matteopaciolla.prbe.util.PasswordResetToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private EmailSenderFacade emailSenderFacade;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private OAuth2AuthorizationRevocationService authorizationRevocationService;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private UserIdentityMergeService userIdentityMergeService;
    @Mock
    private SentinelService sentinelService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void configureTelegramUnificationRetries() {
        ReflectionTestUtils.setField(userService, "TELEGRAM_UNIFICATION_MAX_RETRIES", 3);
    }

    @Test
    void fakeNames_haveAtLeastFiftyOptions() {
        assertThat(FakeNames.FIRST_NAMES).hasSizeGreaterThanOrEqualTo(50);
        assertThat(FakeNames.LAST_NAMES).hasSizeGreaterThanOrEqualTo(50);
    }

    @Test
    void registerWebUser_requiresUsernameEmailAndPassword() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setEmail("alice@example.com");
        request.setPassword("valid-password");
        when(userRepository.findByUsernameOrEmailOrTelegramId("alice42", "alice@example.com", ""))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpRepository.findFirstByEmailAndFlowTypeOrderByCreatedAtDesc("alice@example.com", OtpEntity.FlowType.EMAIL_CONFIRMATION))
                .thenReturn(Optional.empty());

        UserDto result = userService.registerUser(request, false);

        assertThat(result.getUsername()).isEqualTo("alice42");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(FakeNames.FIRST_NAMES).contains(result.getFirstName());
        assertThat(result.getLastName()).isNotBlank();
        verify(passwordEncoder).encode(any());
        verify(emailSenderFacade).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    void registerBotUser_requiresUsernameAndTelegramIdAndGeneratesMissingNames() {
        UserReqDto request = new UserReqDto();
        request.setUsername("pirate42");
        request.setTelegramId("123456789");
        when(userRepository.findByUsernameOrEmailOrTelegramId("pirate42", "", "123456789"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.registerUser(request, true);

        assertThat(result.getTelegramId()).isEqualTo("123456789");
        assertThat(FakeNames.FIRST_NAMES).contains(result.getFirstName());
        assertThat(result.getLastName()).isNotBlank();
    }

    @Test
    void registerWebUser_reportsEmailAsRequired() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setPassword("valid-password");

        assertThatThrownBy(() -> userService.registerUser(request, false))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getMissingParams())
                .isEqualTo(java.util.List.of("email"));
    }

    @Test
    void registerWebUser_rejectsPasswordBelowMinimum() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setEmail("alice@example.com");
        request.setPassword("short7");

        assertThatThrownBy(() -> userService.registerUser(request, false))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getMissingParams())
                .isEqualTo(java.util.List.of("password"));
    }

    @Test
    void registerWebUser_rejectsUnverifiedTelegramIdentity() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setEmail("alice@example.com");
        request.setPassword("valid-password");
        request.setTelegramId("123456789");

        assertThatThrownBy(() -> userService.registerUser(request, false))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getDescription())
                .isEqualTo("Telegram identity must be linked through the authorized account flow");
    }

    @Test
    void registerBotUser_rejectsEmailAndPasswordFields() {
        UserReqDto request = new UserReqDto();
        request.setUsername("pirate42");
        request.setTelegramId("123456789");
        request.setEmail("pirate@example.com");

        assertThatThrownBy(() -> userService.registerUser(request, true))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getDescription())
                .isEqualTo("Telegram registration must not provide email or password");
    }

    @Test
    void registerWebUser_doesNotRevealWhichIdentifierAlreadyExists() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setEmail("alice@example.com");
        request.setPassword("valid-password");
        when(userRepository.findByUsernameOrEmailOrTelegramId("alice42", "alice@example.com", ""))
                .thenReturn(Optional.of(new UserEntity()));

        assertThatThrownBy(() -> userService.registerUser(request, false))
                .isInstanceOf(UniqueConstraintViolatedException.class)
                .extracting(exception -> ((UniqueConstraintViolatedException) exception).getDescription())
                .isEqualTo("Registration could not be completed");
    }

    @Test
    void registerBotUser_reportsTelegramIdAsRequired() {
        UserReqDto request = new UserReqDto();
        request.setUsername("pirate42");

        assertThatThrownBy(() -> userService.registerUser(request, true))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getMissingParams())
                .isEqualTo(java.util.List.of("telegramId"));
    }

    @Test
    void telegramUnificationOtpIsConsumedAfterThreeFailedAttempts() {
        OtpEntity otp = new OtpEntity(
                OtpEntity.FlowType.TELEGRAM_UNIFICATION,
                "alice@example.com",
                "123456789",
                com.matteopaciolla.prbe.util.PasswordResetToken.hash("1234"),
                java.time.LocalDateTime.now().plusHours(1));
        otp.setFailedAttempts(2);
        when(otpRepository.findFirstByEmailAndFlowTypeForUpdate(
                "alice@example.com", OtpEntity.FlowType.TELEGRAM_UNIFICATION)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> userService.unifyTelegramAndEmailAccounts(
                "alice@example.com", "123456789", "wrong"))
                .isInstanceOf(com.matteopaciolla.prbe.exceptions.user.InvalidTelegramUnificationTokenException.class);

        assertThat(otp.getFailedAttempts()).isEqualTo(3);
        assertThat(otp.getConfirmedAt()).isNull();
        verify(otpRepository).save(otp);
    }

    @Test
    void telegramUnificationRejectsOverlappingMatchHistoryWithoutConsumingOtp() {
        OtpEntity otp = new OtpEntity(
                OtpEntity.FlowType.TELEGRAM_UNIFICATION,
                "alice@example.com",
                "123456789",
                com.matteopaciolla.prbe.util.PasswordResetToken.hash("1234"),
                java.time.LocalDateTime.now().plusHours(1));
        otp.setFailedAttempts(0);
        UserEntity telegramUser = new UserEntity("telegram42", "encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        telegramUser.setId(2L);
        telegramUser.setEnabled(true);
        UserEntity webUser = new UserEntity("alice42", "encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        webUser.setId(1L);
        webUser.setEmail("alice@example.com");
        webUser.setEnabled(true);
        webUser.setEmailConfirmed(true);
        when(otpRepository.findFirstByEmailAndFlowTypeForUpdate(
                "alice@example.com", OtpEntity.FlowType.TELEGRAM_UNIFICATION)).thenReturn(Optional.of(otp));
        when(userRepository.findByTelegramId("123456789")).thenReturn(Optional.of(telegramUser));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(webUser));
        when(matchRepository.hasOverlappingMatches(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.unifyTelegramAndEmailAccounts(
                "alice@example.com", "123456789", "1234"))
                .isInstanceOf(MandatoryParamException.class);

        assertThat(otp.getConfirmedAt()).isNull();
        verify(userRepository, never()).delete(telegramUser);
    }

    @Test
    void telegramUnificationTransfersNonOverlappingHistoryToTheConfirmedAccount() {
        OtpEntity otp = new OtpEntity(
                OtpEntity.FlowType.TELEGRAM_UNIFICATION,
                "alice@example.com",
                "123456789",
                com.matteopaciolla.prbe.util.PasswordResetToken.hash("1234"),
                java.time.LocalDateTime.now().plusHours(1));
        otp.setFailedAttempts(0);
        UserEntity telegramUser = new UserEntity("telegram42", "encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        telegramUser.setId(2L);
        telegramUser.setEnabled(true);
        UserEntity webUser = new UserEntity("alice42", "encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        webUser.setId(1L);
        webUser.setEmail("alice@example.com");
        webUser.setEnabled(true);
        webUser.setEmailConfirmed(true);
        when(otpRepository.findFirstByEmailAndFlowTypeForUpdate(
                "alice@example.com", OtpEntity.FlowType.TELEGRAM_UNIFICATION)).thenReturn(Optional.of(otp));
        when(userRepository.findByTelegramId("123456789")).thenReturn(Optional.of(telegramUser));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(webUser));
        when(userRepository.save(webUser)).thenReturn(webUser);

        UserDto result = userService.unifyTelegramAndEmailAccounts("alice@example.com", "123456789", "1234");

        assertThat(result.getUsername()).isEqualTo("alice42");
        assertThat(webUser.getTelegramId()).isEqualTo("123456789");
        verify(userIdentityMergeService).transferHistory(2L, 1L, "telegram42", "alice42");
        verify(userRepository).delete(telegramUser);
        verify(sentinelService).stopUserSubscriptions(2L);
        verify(otpRepository).deleteAllByEmailAndFlowType("alice@example.com", OtpEntity.FlowType.TELEGRAM_UNIFICATION);
        verify(otpRepository).deleteAllByTelegramId("123456789");
    }

    @Test
    void changePassword_revokesAuthorizationsForTheUser() {
        UserEntity user = new UserEntity("alice42", "old-encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        when(userRepository.findByUsername("alice42")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword("alice42", "old-password", "new-password");

        assertThat(user.getCredentialVersion()).isEqualTo(1);
        verify(authorizationRevocationService).revokePrincipal("alice42");
    }

    @Test
    void changePassword_rejectsPasswordBelowMinimum() {
        assertThatThrownBy(() -> userService.changePassword("alice42", "old-password", "short7"))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getMissingParams())
                .isEqualTo(java.util.List.of("newPassword"));
    }

    @Test
    void requestPasswordResetUsesSameGenericPathForUnknownAddresses() {
        when(userRepository.findByEmailAndEnabledAndEmailConfirmed("unknown@example.com", true, true))
                .thenReturn(Optional.empty());

        userService.requestPasswordReset("unknown@example.com");

        verify(emailSenderFacade, never()).sendEmail(any(), any(), any(), any(), any());
        verify(otpRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void requestPasswordResetStoresOnlyHashAndQueuesOneHourToken() {
        UserEntity user = new UserEntity("alice42", "encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        user.setEmail("alice@example.com");
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        when(userRepository.findByEmailAndEnabledAndEmailConfirmed("alice@example.com", true, true))
                .thenReturn(Optional.of(user));
        ArgumentCaptor<OtpEntity> savedOtp = ArgumentCaptor.forClass(OtpEntity.class);
        when(otpRepository.save(savedOtp.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<String> textBody = ArgumentCaptor.forClass(String.class);

        userService.requestPasswordReset("alice@example.com");

        OtpEntity token = savedOtp.getValue();
        assertThat(token.getFlowType()).isEqualTo(OtpEntity.FlowType.PASSWORD_RESET);
        assertThat(token.getToken()).matches("[0-9a-f]{64}");
        assertThat(token.getExpiresAt()).isAfter(java.time.LocalDateTime.now().plusMinutes(59));
        assertThat(token.getExpiresAt()).isBefore(java.time.LocalDateTime.now().plusHours(1).plusSeconds(1));
        verify(emailSenderFacade).sendEmail(
                eq("alice@example.com"), eq("alice42"), eq("Password Reset"), any(), textBody.capture());
        assertThat(textBody.getValue()).contains("one-time password reset token");
        assertThat(textBody.getValue()).doesNotContain(token.getToken());
    }

    @Test
    void resetPasswordConsumesTokenAndRevokesExistingAuthorizations() {
        String rawToken = PasswordResetToken.generate();
        OtpEntity reset = new OtpEntity(
                OtpEntity.FlowType.PASSWORD_RESET,
                "alice@example.com",
                PasswordResetToken.hash(rawToken),
                java.time.LocalDateTime.now().plusHours(1));
        UserEntity user = new UserEntity("alice42", "old-encoded", java.util.List.of(
                com.matteopaciolla.prbe.constants.enums.UserRole.USER));
        user.setEmail("alice@example.com");
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        when(otpRepository.findFirstByTokenAndFlowTypeForUpdate(
                reset.getToken(), OtpEntity.FlowType.PASSWORD_RESET)).thenReturn(Optional.of(reset));
        when(userRepository.findByEmailAndEnabledAndEmailConfirmed("alice@example.com", true, true))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("a-valid-password")).thenReturn("new-encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.resetPassword(rawToken, "a-valid-password");

        assertThat(reset.getConfirmedAt()).isNotNull();
        assertThat(user.getPassword()).isEqualTo("new-encoded");
        assertThat(user.getCredentialVersion()).isEqualTo(1);
        verify(authorizationRevocationService).revokePrincipal("alice42");
    }

    @Test
    void registerUser_preservesProvidedNameAndGeneratesOnlyMissingName() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setTelegramId("123456789");
        request.setFirstName("Alice");
        when(userRepository.findByUsernameOrEmailOrTelegramId("alice42", "", "123456789"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        ArgumentCaptor<UserEntity> savedUser = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(savedUser.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request, true);

        assertThat(savedUser.getValue().getFirstName()).isEqualTo("Alice");
        assertThat(savedUser.getValue().getLastName()).isNotBlank();
    }
}
