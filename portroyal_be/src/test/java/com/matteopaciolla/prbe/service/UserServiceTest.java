package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.FakeNames;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.dto.request.UserReqDto;
import com.matteopaciolla.prbe.exceptions.common.MandatoryParamException;
import com.matteopaciolla.prbe.facade.EmailSenderFacade;
import com.matteopaciolla.prbe.model.entity.OtpEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.OtpRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @InjectMocks
    private UserService userService;

    @Test
    void fakeNames_haveAtLeastFiftyOptions() {
        assertThat(FakeNames.FIRST_NAMES).hasSizeGreaterThanOrEqualTo(50);
        assertThat(FakeNames.LAST_NAMES).hasSizeGreaterThanOrEqualTo(50);
    }

    @Test
    void registerWebUser_requiresUsernameAndEmailButNotPassword() {
        UserReqDto request = new UserReqDto();
        request.setUsername("alice42");
        request.setEmail("alice@example.com");
        when(userRepository.findByUsernameOrEmailOrTelegramId("alice42", "alice@example.com", ""))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpRepository.findFirstByEmailAndFlowType("alice@example.com", OtpEntity.FlowType.EMAIL_CONFIRMATION))
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

        assertThatThrownBy(() -> userService.registerUser(request, false))
                .isInstanceOf(MandatoryParamException.class)
                .extracting(exception -> ((MandatoryParamException) exception).getMissingParams())
                .isEqualTo(java.util.List.of("email"));
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

    @Test
    void updateUser_usesPathUsernameWhenBodyOmitsUsername() {
        UserEntity alice = new UserEntity("alice42", "encoded-password", java.util.List.of(UserRole.USER));
        alice.setFirstName("Alice");
        UserReqDto request = new UserReqDto();
        request.setFirstName("Alicia");
        when(userRepository.findByUsername("alice42")).thenReturn(Optional.of(alice));
        when(userRepository.save(alice)).thenReturn(alice);

        userService.updateUser("alice42", request);

        assertThat(alice.getFirstName()).isEqualTo("Alicia");
        verify(userRepository).findByUsername("alice42");
        verify(userRepository).save(alice);
    }

    @Test
    void updateUser_rejectsUsernameThatDoesNotMatchPath() {
        UserReqDto request = new UserReqDto();
        request.setUsername("bob42");

        assertThatThrownBy(() -> userService.updateUser("alice42", request))
                .isInstanceOf(MandatoryParamException.class);

        verifyNoInteractions(userRepository);
    }
}
