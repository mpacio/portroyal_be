package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.CallbackEntity;
import com.matteopaciolla.prbe.model.entity.CallbackEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CallbackRepository extends JpaRepository<CallbackEntity, CallbackEntityId> {

    List<CallbackEntity> findByMatchKeyCode(String matchKeyCode);
    Optional<CallbackEntity> findByUserIdAndMatchKeyCode(Long userId, String matchKeyCode);

    @Query("SELECT DISTINCT c.url FROM CallbackEntity c WHERE c.matchKeyCode = :keyCode")
    Set<String> findDistinctUrlsByMatchKeyCode(String keyCode);

    @Query("SELECT DISTINCT c.url AS url, c.secret AS secret FROM CallbackEntity c WHERE c.matchKeyCode = :keyCode")
    Set<UrlSecret> findDistinctUrlSecretByMatchKeyCode(String keyCode);

    interface UrlSecret {
        String getUrl();
        String getSecret();
    }
}
