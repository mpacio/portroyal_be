package com.matteopaciolla.prbe.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@IdClass(CallbackEntityId.class)
@Table(name = "callbacks")
public class CallbackEntity {

    @Id
    @Column(nullable = false)
    private String matchKeyCode;
    @Id
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String url;
    private String secret;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public CallbackEntity(String keyCode, Long userId, String url, String secret) {
        this.matchKeyCode = keyCode;
        this.userId = userId;
        this.url = url;
        this.secret = secret;
    }
}
