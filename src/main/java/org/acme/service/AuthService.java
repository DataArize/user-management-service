package org.acme.service;

import com.password4j.Password;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.AttemptType;
import org.acme.constants.Entity;
import org.acme.constants.Errors;
import org.acme.entity.LoginAttempts;
import org.acme.entity.Token;
import org.acme.entity.User;
import org.acme.exceptions.AccountDoesNotExistsException;
import org.acme.exceptions.InvalidLoginCredentialsException;
import org.acme.exceptions.RegistrationFailedException;
import org.acme.exceptions.UnableToPresistException;
import org.acme.model.Auth;
import org.acme.model.AuthResponse;
import org.acme.utils.JwtUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.jose4j.jwk.Use;

import java.time.LocalDateTime;

@Slf4j
@ApplicationScoped
public class AuthService {

    @ConfigProperty(name = "jwt.expiration")
    private Long expiration;

    public Uni<Auth> createUser(Auth authRequest) {
        User user = new User(authRequest);
        String passwordHash = Password.hash(authRequest.getPassword()).withArgon2().getResult();
        user.setPasswordHash(passwordHash);
        return Panache.withTransaction(() -> PanacheEntityBase.persist(user))
            .onItem().transform(success -> {
                log.info("Successfully created user, email: {}", authRequest.getEmail());
                return authRequest;
            })
                .onFailure(ConstraintViolationException.class)
                .transform(ex -> {
                    log.error("User already exists, email: {}", user.getEmail());
                    throw new AccountDoesNotExistsException(Errors.ACCOUNT_ALREADY_EXISTS + user.getEmail());
                });
    }

    public Uni<AuthResponse> authenticateUser(Auth authRequest) {
        return findByUsernameAndPassword(authRequest)
                .onItem().transformToUni(user -> {
                    LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(expiration * 24 * 30);
                    String accessToken = JwtUtils.generateAccessToken(user, expiration);
                    String refreshToken = JwtUtils.generateRefreshToken(user, expiration * 24 * 30);
                    return persistToken(user, refreshToken, expirationTime)
                            .onItem().transform(success -> {
                                AuthResponse authResponse = new AuthResponse();
                                authResponse.setAccessToken(accessToken);
                                authResponse.setRefreshToken(refreshToken);
                                authResponse.setExpiresIn(expiration);
                                return authResponse;
                            });
                });
    }

    public Uni<User> findByUsernameAndPassword(Auth authRequest) {

        return User.find(Entity.EMAIL, authRequest.getEmail()).firstResult()
                .onItem().ifNotNull().transformToUni(entity -> {
                    User user = (User) entity;
                    return validatePassword(authRequest.getPassword(), user)
                            .onItem().transform(success -> {
                                if(Boolean.TRUE.equals(success)) return user;
                                throw new InvalidLoginCredentialsException(Errors.INVALID_LOGIN_CREDENTIALS);
                            });
                })
                .onItem().ifNull().failWith(new AccountDoesNotExistsException(Errors.ACCOUNT_DOES_NOT_EXISTS));
    }

    private Uni<Boolean> validatePassword(String password, User user) {
        boolean validPassword = Password.check(password, user.getPasswordHash()).withArgon2();
        if (validPassword) {
            return persistLoginAttempt(user, AttemptType.SUCCESS)
                    .onItem().transform(ignored -> true);
        } else {
            return persistLoginAttempt(user, AttemptType.FAILED)
                    .onItem().transform(ignored -> false);
        }
    }

    public Uni<Void> persistLoginAttempt(User user, AttemptType attempt) {
        LoginAttempts loginAttempts = new LoginAttempts(user,
                attempt.name().equalsIgnoreCase(AttemptType.SUCCESS.name()));

        return Panache.withTransaction(() -> PanacheEntityBase.persist(loginAttempts))
                .onItem().transform(success -> {
                    log.info("Persisting user login attempt data, Result: {}, email: {}",attempt, user.getEmail());
                    return Uni.createFrom().voidItem();
                })
                .onFailure().transform(ex -> {
                    log.error("Unable to persist user login attempt, email: {}", user.getEmail());
                    throw new UnableToPresistException(Errors.UNABLE_TO_PERSIST_LOGIN_ATTEMPT_DETAILS + ex.getMessage());
                })
                .replaceWithVoid();
    }

    public Uni<Void> persistToken(User user, String refreshToken, LocalDateTime expiration) {
        Token token = new Token(user, refreshToken, expiration);
        return Panache.withTransaction(() -> PanacheEntityBase.persist(token))
                .onItem().transform(success -> {
                    log.info("Persisting refresh token details for user: {}", user.getEmail());
                    return Uni.createFrom().voidItem();
                }).onFailure().transform(ex -> {
                    log.error("Unable to persist refresh token for user: {}", user.getEmail());
                    throw new UnableToPresistException(Errors.UNABLE_TO_PERSIST_TOKEN_DETAILS + ex.getMessage());
                }).replaceWithVoid();
    }
}
