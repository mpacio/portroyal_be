package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.MatchEntity;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<MatchEntity> findByHostUserTelegramIdAndEnded(String username, boolean ended);

    Optional<MatchEntity> findFirstByHostUserUsernameAndEnded(String username, boolean ended);

    Optional<MatchEntity> findFirstByHostUserTelegramIdAndEnded(String username, boolean ended);

    Page<MatchEntity> findByPlayersUsername(String username, Pageable pageable);

    Page<MatchEntity> findByPlayersUsernameAndEnded(String username, boolean ended, Pageable pageable);

    Page<MatchEntity> findByPlayersTelegramIdAndEnded(String telegramId, boolean ended, Pageable pageable);

    List<MatchEntity> findByWinnerUsername(String username);

    Page<MatchEntity> findByEnded(Boolean ended, Pageable pageable);

}
