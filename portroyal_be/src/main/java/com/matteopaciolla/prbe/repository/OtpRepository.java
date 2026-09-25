package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findFirstByTokenAndFlowType(String token, OtpEntity.FlowType flowType);
    Optional<OtpEntity> findFirstByEmailAndFlowType(String email, OtpEntity.FlowType flowType);

    @Modifying
    @Transactional
    void deleteAllByEmailAndFlowType(String email, OtpEntity.FlowType flowType);
}
