package com.matteopaciolla.prbe.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

public class RotatingOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationService delegate;
    private final JdbcTemplate jdbcTemplate;

    public RotatingOAuth2AuthorizationService(
            @Qualifier("jdbcOAuth2AuthorizationService") OAuth2AuthorizationService delegate,
            JdbcTemplate jdbcTemplate) {
        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(OAuth2Authorization authorization) {
        if (hasRevokedFamily(authorization.getId())) {
            throw new IllegalStateException("A refresh-token family was revoked after suspected replay.");
        }
        delegate.save(authorization);
        OAuth2Authorization.Token<?> refreshToken = authorization.getRefreshToken();
        if (refreshToken == null) {
            return;
        }

        insertHistory(
                hash(refreshToken.getToken().getTokenValue()),
                authorization.getId(),
                authorization.getPrincipalName(),
                authorization.getRegisteredClientId(),
                "ACTIVE",
                refreshToken.getToken().getIssuedAt() == null
                        ? Instant.now()
                        : refreshToken.getToken().getIssuedAt());
    }

    @Override
    @Transactional
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
        jdbcTemplate.update("""
                UPDATE oauth2_refresh_token_history
                SET status = 'REVOKED'
                WHERE authorization_id = ?
                """, authorization.getId());
    }

    @Override
    @Transactional
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    @Transactional
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return delegate.findByToken(token, tokenType);
        }

        String tokenHash = hash(token);
        OAuth2Authorization authorization = delegate.findByToken(token, tokenType);
        if (authorization != null) {
            int claimed = jdbcTemplate.update("""
                            UPDATE oauth2_refresh_token_history
                            SET status = 'USED'
                            WHERE token_hash = ? AND status = 'ACTIVE'
                            """,
                    tokenHash);
            if (claimed == 1) {
                return authorization;
            }
            if (claimed == 0 && !hasHistory(tokenHash)) {
                OAuth2Authorization.Token<?> refreshToken = authorization.getRefreshToken();
                insertHistory(
                        tokenHash,
                        authorization.getId(),
                        authorization.getPrincipalName(),
                        authorization.getRegisteredClientId(),
                        "USED",
                        refreshToken == null || refreshToken.getToken().getIssuedAt() == null
                                ? Instant.now()
                                : refreshToken.getToken().getIssuedAt());
                return authorization;
            }
            revokeFamily(authorization.getId());
            return null;
        }

        List<String> familyIds = jdbcTemplate.query(
                "SELECT authorization_id FROM oauth2_refresh_token_history WHERE token_hash = ?",
                (resultSet, rowNumber) -> resultSet.getString(1),
                tokenHash);
        if (!familyIds.isEmpty()) {
            revokeFamily(familyIds.getFirst());
        }
        return null;
    }

    private boolean hasHistory(String tokenHash) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_refresh_token_history WHERE token_hash = ?",
                Integer.class,
                tokenHash);
        return count != null && count > 0;
    }

    private void insertHistory(
            String tokenHash,
            String authorizationId,
            String principalName,
            String clientId,
            String status,
            Instant issuedAt) {
        if (hasHistory(tokenHash)) {
            return;
        }
        jdbcTemplate.update("""
                        INSERT INTO oauth2_refresh_token_history
                            (token_hash, authorization_id, principal_name, client_id, status, issued_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                tokenHash, authorizationId, principalName, clientId, status, issuedAt);
    }

    private boolean hasRevokedFamily(String authorizationId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM oauth2_refresh_token_history
                        WHERE authorization_id = ? AND status = 'REVOKED'
                        """,
                Integer.class,
                authorizationId);
        return count != null && count > 0;
    }

    private void revokeFamily(String authorizationId) {
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", authorizationId);
        jdbcTemplate.update("""
                UPDATE oauth2_refresh_token_history
                SET status = 'REVOKED'
                WHERE authorization_id = ?
                """, authorizationId);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
