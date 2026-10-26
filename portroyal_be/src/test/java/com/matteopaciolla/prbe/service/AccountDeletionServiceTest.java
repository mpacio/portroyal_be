package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.EmailRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.OtpRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private OAuth2AuthorizationRevocationService authorizationRevocationService;
    @Mock
    private SentinelService sentinelService;
    @Mock
    private MoveHistoryIdentityService moveHistoryIdentityService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AccountDeletionService accountDeletionService;

    @Test
    void requestDeletionDefersAndRetainsAccountWhileMatchesAreActive() {
        UserEntity user = user();
        when(userRepository.findByUsernameForUpdate("alice42")).thenReturn(Optional.of(user));
        when(matchRepository.hasActiveMatchForUser(42L)).thenReturn(true);

        AccountDeletionService.RequestStatus result = accountDeletionService.requestDeletion("alice42");

        assertThat(result).isEqualTo(AccountDeletionService.RequestStatus.PENDING_MATCHES);
        assertThat(user.getDeletionRequestedAt()).isNotNull();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void requestDeletionPseudonymizesProfileAndRevokesIdentityWhenNoMatchIsActive() {
        UserEntity user = user();
        when(userRepository.findByUsernameForUpdate("alice42")).thenReturn(Optional.of(user));
        when(matchRepository.hasActiveMatchForUser(42L)).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("random-password-hash");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDeletionService.RequestStatus result = accountDeletionService.requestDeletion("alice42");

        assertThat(result).isEqualTo(AccountDeletionService.RequestStatus.COMPLETED);
        assertThat(user.getUsername()).startsWith("deleted-").isNotEqualTo("alice42");
        assertThat(user.getEmail()).isNull();
        assertThat(user.getTelegramId()).isNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isEmailConfirmed()).isFalse();
        assertThat(user.getDeletionRequestedAt()).isNull();
        assertThat(user.getCredentialVersion()).isEqualTo(1);
        assertThat(user.getRoles()).containsExactly(UserRole.USER);
        verify(authorizationRevocationService).revokePrincipal("alice42");
        verify(authorizationRevocationService).revokePrincipalByUserId("42");
        verify(otpRepository).deleteAllByEmail("alice@example.com");
        verify(emailRepository).deleteAllByRecipientEmail("alice@example.com");
        verify(emailRepository).deleteAllByRecipientName("alice42");
        verify(otpRepository).deleteAllByTelegramId("123456789");
        verify(sentinelService).removeUserSubscriptions(42L);
        verify(moveHistoryIdentityService).renamePlayer("alice42", user.getUsername());
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity("alice42", "encoded", List.of(UserRole.USER));
        user.setId(42L);
        user.setEmail("alice@example.com");
        user.setTelegramId("123456789");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        return user;
    }
}
