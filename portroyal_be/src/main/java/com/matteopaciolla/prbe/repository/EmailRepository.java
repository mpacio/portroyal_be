package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.EmailQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailRepository extends JpaRepository<EmailQueueEntity, Long> {

    long countBySentFalse();

    List<EmailQueueEntity> findBySentFalseAndCreatedAtBefore(LocalDateTime createdAt);

    @Transactional
    void deleteAllByRecipientEmail(String recipientEmail);

    @Transactional
    void deleteAllByRecipientName(String recipientName);

    // query to get the number of emails sent between two dates
    Integer countBySentTrueAndSentAtBetween(LocalDateTime start, LocalDateTime end);

    // query to get the number of emails sent after a specific datetime
    Integer countBySentTrueAndSentAtAfter(LocalDateTime start);
}
