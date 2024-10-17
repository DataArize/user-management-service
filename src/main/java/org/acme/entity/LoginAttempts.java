package org.acme.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @ManyToOne()
    @JoinColumn(name = "loginAttempts")
    private User users;
    @CreationTimestamp
    private LocalDateTime attemptTime;
    private boolean success;

    public LoginAttempts(User user, boolean attemptType) {
        this.users = user;
        this.success = attemptType;
    }
}
