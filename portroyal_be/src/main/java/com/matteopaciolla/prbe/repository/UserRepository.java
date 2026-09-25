package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByTelegramId(String telegramId);

    Optional<UserEntity> findByUsernameAndEnabled(String username, boolean enabled);

    Optional<UserEntity> findByUsernameAndEnabledAndEmailConfirmed(String username, boolean enabled, boolean emailConfirmed);

    Optional<UserEntity> findByEmailAndEnabled(String email, boolean enabled);

    Optional<UserEntity> findByTelegramIdAndEnabled(String telegramId, boolean enabled);

    Optional<UserEntity> findByUsernameOrEmailOrTelegramId(String username, String email, String telegramId);

    @Modifying
    @Transactional
    void deleteAllByTelegramId(String telegramId);
}
