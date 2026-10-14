package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findFirstByTokenAndFlowTypeOrderByCreatedAtDesc(String token, OtpEntity.FlowType flowType);
    Optional<OtpEntity> findFirstByEmailAndFlowTypeOrderByCreatedAtDesc(String email, OtpEntity.FlowType flowType);

    @Modifying
    @Transactional
    void deleteAllByEmailAndFlowType(String email, OtpEntity.FlowType flowType);
}
