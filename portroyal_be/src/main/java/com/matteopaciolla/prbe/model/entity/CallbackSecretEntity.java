package com.matteopaciolla.prbe.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@IdClass(CallbackEntityId.class)
@Table(name = "callback_secrets")
public class CallbackSecretEntity {

    @Id
    @Column(nullable = false)
    private String matchKeyCode;

    @Id
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String encryptedSecret;

    public CallbackSecretEntity(String matchKeyCode, Long userId, String encryptedSecret) {
        this.matchKeyCode = matchKeyCode;
        this.userId = userId;
        this.encryptedSecret = encryptedSecret;
    }
}
