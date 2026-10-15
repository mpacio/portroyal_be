package com.matteopaciolla.prbe.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthAuthorizationRevocationService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void revokeUserAuthorizations(String username) {
        jdbcTemplate.update("""
                UPDATE oauth2_refresh_token_history
                SET status = 'REVOKED'
                WHERE principal_name = ?
                """, username);
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name = ?", username);
        jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE principal_name = ?", username);
    }
}
