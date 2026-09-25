package com.matteopaciolla.prbe.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "temporary_tokens")
public class OtpEntity {

    public enum FlowType {
        EMAIL_CONFIRMATION,
        TELEGRAM_UNIFICATION,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "temporary_tokens_id_generator")
    @SequenceGenerator(name = "temporary_tokens_id_generator", sequenceName = "temporary_tokens_id_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String token;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FlowType flowType;
    private String email;
    private String telegramId;
    private Integer failedAttempts = 0;
    private LocalDateTime confirmedAt;
    private LocalDateTime expiresAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public OtpEntity(FlowType flowType, String email, String token, LocalDateTime expiresAt) {
        this.flowType = flowType;
        this.email = email;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public OtpEntity(FlowType flowType, String email, String telegramId, String token, LocalDateTime expiresAt) {
        this.flowType = flowType;
        this.email = email;
        this.telegramId = telegramId;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
