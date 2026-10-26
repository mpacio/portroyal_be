package com.matteopaciolla.prbe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserIdentityMergeService {

    private final JdbcTemplate jdbcTemplate;
    private final MoveHistoryIdentityService moveHistoryIdentityService;

    public void transferHistory(Long sourceUserId, Long targetUserId, String sourceUsername, String targetUsername) {
        jdbcTemplate.update(
                """
                DELETE FROM callbacks source
                WHERE source.user_id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM callbacks target
                      WHERE target.user_id = ?
                        AND target.match_key_code = source.match_key_code
                  )
                """,
                sourceUserId, targetUserId);
        jdbcTemplate.update(
                "UPDATE match_user SET user_id = ? WHERE user_id = ?", targetUserId, sourceUserId);
        jdbcTemplate.update(
                "UPDATE matches SET host_user_id = ? WHERE host_user_id = ?", targetUserId, sourceUserId);
        jdbcTemplate.update(
                "UPDATE matches SET winner_user_id = ? WHERE winner_user_id = ?", targetUserId, sourceUserId);
        jdbcTemplate.update(
                "UPDATE callbacks SET user_id = ? WHERE user_id = ?", targetUserId, sourceUserId);
        moveHistoryIdentityService.renamePlayer(sourceUsername, targetUsername);
    }
}
