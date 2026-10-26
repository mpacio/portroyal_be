package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    @Getter
    enum SortField {
        CREATED_AT("createdAt"),
        STARTED_AT("startedAt"),
        ENDED_AT("endedAt"),
        ;

        private final String fieldName;

        SortField(String fieldName) {
            this.fieldName = fieldName;
        }

    }

    List<MatchEntity> findByHostUserId(Long hostId);

    List<MatchEntity> findByHostUser(UserEntity hostUser);

    Optional<MatchEntity> findByKeyCode(String keyCode);

    List<MatchEntity> findByHostUserUsername(String username);

    List<MatchEntity> findByHostUserUsernameAndEnded(String username, boolean ended);

    Optional<MatchEntity> findFirstByHostUserUsernameAndEnded(String username, boolean ended);

    Page<MatchEntity> findByPlayersUsername(String username, Pageable pageable);

    Page<MatchEntity> findByPlayersUsernameAndEnded(String username, boolean ended, Pageable pageable);

    List<MatchEntity> findByWinnerUsername(String username);

    @Query("""
            select case when count(m.id) > 0 then true else false end
            from MatchEntity m
            left join m.players player
            where m.ended = false
              and (m.hostUser.id = :userId or player.id = :userId)
            """)
    boolean hasActiveMatchForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM matches m
                WHERE (
                    m.host_user_id = :sourceId
                    OR m.winner_user_id = :sourceId
                    OR EXISTS (
                        SELECT 1 FROM match_user source_player
                        WHERE source_player.match_id = m.id AND source_player.user_id = :sourceId
                    )
                )
                AND (
                    m.host_user_id = :targetId
                    OR m.winner_user_id = :targetId
                    OR EXISTS (
                        SELECT 1 FROM match_user target_player
                        WHERE target_player.match_id = m.id AND target_player.user_id = :targetId
                    )
                )
            )
            """, nativeQuery = true)
    boolean hasOverlappingMatches(
            @Param("sourceId") Long sourceId,
            @Param("targetId") Long targetId);

    Page<MatchEntity> findByEnded(Boolean ended, Pageable pageable);

}
