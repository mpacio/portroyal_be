package com.matteopaciolla.prbe.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "emails_queue")
public class EmailQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "emails_queue_id_generator")
    @SequenceGenerator(name = "emails_queue_id_generator", sequenceName = "emails_queue_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean sent = false;
    private LocalDateTime sentAt;
    @NotNull
    @Email
    @Column(nullable = false)
    private String recipientEmail;
    @Column(nullable = false)
    private String recipientName;
    @NotNull
    @Column(nullable = false)
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String htmlBody;
    @Column(columnDefinition = "TEXT")
    private String textBody;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
