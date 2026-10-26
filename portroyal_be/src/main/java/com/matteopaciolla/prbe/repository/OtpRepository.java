package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.OtpEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findFirstByTokenAndFlowTypeOrderByCreatedAtDesc(String token, OtpEntity.FlowType flowType);
    Optional<OtpEntity> findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(String email, OtpEntity.FlowType flowType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select otp from OtpEntity otp where otp.email = :email and otp.flowType = :flowType order by otp.createdAt desc")
    Optional<OtpEntity> findFirstByEmailAndFlowTypeForUpdate(
            @Param("email") String email,
            @Param("flowType") OtpEntity.FlowType flowType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select otp from OtpEntity otp where otp.token = :token and otp.flowType = :flowType order by otp.createdAt desc")
    Optional<OtpEntity> findFirstByTokenAndFlowTypeForUpdate(
            @Param("token") String token,
            @Param("flowType") OtpEntity.FlowType flowType);

    @Modifying
    @Transactional
    void deleteAllByEmailAndFlowType(String email, OtpEntity.FlowType flowType);

    @Modifying
    @Transactional
    void deleteAllByEmail(String email);

    @Modifying
    @Transactional
    void deleteAllByTelegramId(String telegramId);
}
