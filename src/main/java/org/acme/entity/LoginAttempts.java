package org.acme.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "login_attempts", schema = "user_management")
@EqualsAndHashCode(callSuper = true)
public class LoginAttempts extends PanacheEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;
    @CreationTimestamp
    private LocalDateTime attemptTime;
    private boolean success;

    public LoginAttempts(Long userId, boolean attemptType) {
        this.userId = userId;
        this.success = attemptType;
    }
}
