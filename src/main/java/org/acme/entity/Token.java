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

    @ManyToOne()
    @JoinColumn(name = "tokens")
    private User users;
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    private LocalDateTime expiresAt;

    public Token(User user, String refreshToken, LocalDateTime expiresAt) {
        this.users = user;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
