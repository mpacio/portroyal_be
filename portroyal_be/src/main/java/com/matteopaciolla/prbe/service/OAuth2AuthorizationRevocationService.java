package com.matteopaciolla.prbe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2AuthorizationRevocationService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void revokePrincipal(String principalName) {
        deleteAuthorizations(principalName);
    }

    @Transactional
    public void revokePrincipalByUserId(String userId) {
        long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Token subject must be a numeric user ID", exception);
        }
        String principalName = jdbcTemplate.query(
                "SELECT username FROM users WHERE id = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null,
                id);
        if (principalName != null) {
            deleteAuthorizations(principalName);
        }
    }

    private void deleteAuthorizations(String principalName) {
        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE principal_name = ?",
                principalName);
        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization_consent WHERE principal_name = ?",
                principalName);
    }
}
