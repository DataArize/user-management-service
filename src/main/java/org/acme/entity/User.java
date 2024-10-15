package org.acme.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.acme.constants.AccountRole;
import org.acme.constants.AccountStatus;
import org.acme.constants.DataValidation;
import org.acme.model.Auth;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "user", schema = "user_management")
public class User extends PanacheEntity {


    @Email
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    @Length(min = 8, max = 255)
    @Pattern(regexp = DataValidation.PASSWORD_PATTERN, message = DataValidation.PASSWORD_REQUIREMENTS_MESSAGE)
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String firstName;
    @Column(nullable = false, length = 100)
    private String lastName;
    @Column(columnDefinition = "VARCHAR(20) DEFAULT '10GB'", length = 20)
    private String quota;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountStatus status;
    @CreationTimestamp
    private LocalDateTime createAt = LocalDateTime.now();
    private LocalDateTime lastLogin;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "user_roles",
            schema = "user_management",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    public User() {
        this.roles.add(new Role(AccountRole.USER.name()));
    }

    public User(Auth authRequest) {
        this.email = authRequest.getEmail();
        this.passwordHash = authRequest.getPassword();
        this.firstName = authRequest.getFirstName();
        this.lastName = authRequest.getLastName();
        this.roles.add(new Role(AccountRole.USER.name()));
    }

}
