package org.acme.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "tokens", schema = "user_management")
public class Token extends PanacheEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    private LocalDateTime expiresAt;

    public Token(Long userId, String refreshToken, LocalDateTime expiresAt) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
