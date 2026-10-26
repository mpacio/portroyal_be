package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.EmailRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.repository.OtpRepository;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.util.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final int DELETION_BATCH_SIZE = 50;

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final OtpRepository otpRepository;
    private final EmailRepository emailRepository;
    private final OAuth2AuthorizationRevocationService authorizationRevocationService;
    private final SentinelService sentinelService;
    private final MoveHistoryIdentityService moveHistoryIdentityService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public enum RequestStatus {
        COMPLETED,
        PENDING_MATCHES
    }

    @Transactional
    public RequestStatus requestDeletion(String username) {
        UserEntity user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResourceNotFoundException("The requested user could not be found"));
        if (user.getDeletionRequestedAt() != null) {
            return RequestStatus.PENDING_MATCHES;
        }
        if (matchRepository.hasActiveMatchForUser(user.getId())) {
            user.setDeletionRequestedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Account deletion deferred until active matches finish");
            return RequestStatus.PENDING_MATCHES;
        }

        anonymizeAccount(user);
        return RequestStatus.COMPLETED;
    }

    @Scheduled(fixedDelayString = "${app.account-deletion.scan-interval-ms:60000}")
    @Transactional
    public void processPendingDeletions() {
        List<Long> userIds = jdbcTemplate.query(
                """
                SELECT u.id
                FROM users u
                WHERE u.deletion_requested_at IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM matches m
                      LEFT JOIN match_user mu ON mu.match_id = m.id
                      WHERE m.ended = false
                        AND (m.host_user_id = u.id OR mu.user_id = u.id)
                  )
                ORDER BY u.deletion_requested_at
                LIMIT ?
                FOR UPDATE OF u SKIP LOCKED
                """,
                (resultSet, rowNumber) -> resultSet.getLong(1),
                DELETION_BATCH_SIZE);

        for (Long userId : userIds) {
            userRepository.findById(userId).ifPresent(this::anonymizeAccount);
        }
    }

    private void anonymizeAccount(UserEntity user) {
        String originalUsername = user.getUsername();
        String originalEmail = user.getEmail();
        String pseudonym = createPseudonym();

        authorizationRevocationService.revokePrincipal(originalUsername);
        authorizationRevocationService.revokePrincipalByUserId(user.getId().toString());
        sentinelService.removeUserSubscriptions(user.getId());
        if (originalEmail != null) {
            otpRepository.deleteAllByEmail(originalEmail);
            emailRepository.deleteAllByRecipientEmail(originalEmail);
        }
        if (user.getTelegramId() != null) {
            otpRepository.deleteAllByTelegramId(user.getTelegramId());
        }
        emailRepository.deleteAllByRecipientName(originalUsername);

        moveHistoryIdentityService.renamePlayer(originalUsername, pseudonym);

        user.setUsername(pseudonym);
        user.setPassword(passwordEncoder.encode(PasswordResetToken.generate()));
        user.setFirstName(null);
        user.setLastName(null);
        user.setEmail(null);
        user.setTelegramId(null);
        user.setRoles(List.of(UserRole.USER));
        user.setEnabled(false);
        user.setEmailConfirmed(false);
        user.setLastLogin(null);
        user.setDeletionRequestedAt(null);
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        userRepository.save(user);

        log.info("Account identifying data removed for user record {}", user.getId());
    }

    private String createPseudonym() {
        String pseudonym;
        do {
            pseudonym = "deleted-" + PasswordResetToken.generate().substring(0, 16);
        } while (userRepository.existsByUsername(pseudonym));
        return pseudonym;
    }

}
