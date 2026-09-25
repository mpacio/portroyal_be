package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.MoveEntity;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MoveRepository extends JpaRepository<MoveEntity, Long> {

    @Getter
    enum SortField {
        TIME_INDEX("timeIndex"),
        ACTIVE_PLAYER("activePlayerIndex"),
        RUNNING_PLAYER("runningPlayerIndex"),
        MOVE_NAME("moveName"),
        ACTIVE_PLAYER_USERNAME("activePlayerUsername"),
        RUNNING_PLAYER_USERNAME("runningPlayerUsername");

        private final String fieldName;

        SortField(String fieldName) {
            this.fieldName = fieldName;
        }

    }

    List<MoveEntity> findByMatchIdOrderByTimeIndex(Long matchId);
    Optional<MoveEntity> findByMatchIdAndTimeIndex(Long matchId, Integer timeIndex);
    Page<MoveEntity> findByMatchIdOrderByTimeIndex(Long matchId, Pageable pageable);
    Page<MoveEntity> findByMatchId(Long matchId,Pageable pageable);
}
