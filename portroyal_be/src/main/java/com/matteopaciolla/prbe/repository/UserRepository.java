package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.username = :username")
    Optional<UserEntity> findByUsernameForUpdate(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByTelegramId(String telegramId);

    Optional<UserEntity> findByUsernameAndEnabled(String username, boolean enabled);

    Optional<UserEntity> findByUsernameAndEnabledAndEmailConfirmed(String username, boolean enabled, boolean emailConfirmed);

    Optional<UserEntity> findByEmailAndEnabled(String email, boolean enabled);

    Optional<UserEntity> findByEmailAndEnabledAndEmailConfirmed(String email, boolean enabled, boolean emailConfirmed);

    Optional<UserEntity> findByTelegramIdAndEnabled(String telegramId, boolean enabled);

    Optional<UserEntity> findByUsernameOrEmailOrTelegramId(String username, String email, String telegramId);

}
