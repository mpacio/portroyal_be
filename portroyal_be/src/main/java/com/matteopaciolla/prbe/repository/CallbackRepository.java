package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.CallbackEntity;
import com.matteopaciolla.prbe.model.entity.CallbackEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CallbackRepository extends JpaRepository<CallbackEntity, CallbackEntityId> {

    List<CallbackEntity> findByMatchKeyCode(String matchKeyCode);
    Optional<CallbackEntity> findByUserIdAndMatchKeyCode(Long userId, String matchKeyCode);

}
