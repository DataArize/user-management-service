package org.acme.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@Table(name = "password_resets", schema = "user_management")
public class PasswordResets extends PanacheEntity {

    @Column(nullable = false)
    private Long userId;
    @Column(columnDefinition = "TEXT")
    private String resetToken;
    private LocalDateTime expiresAt;
    @CreationTimestamp
    private LocalDateTime createdAt;

    public PasswordResets(String resetToken, Long userId, LocalDateTime expiresAt) {
        this.resetToken = resetToken;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

}
