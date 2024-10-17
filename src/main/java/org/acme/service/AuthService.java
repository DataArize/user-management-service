package org.acme.service;

import com.password4j.Password;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.AttemptType;
import org.acme.constants.Entity;
import org.acme.constants.Errors;
import org.acme.entity.LoginAttempts;
import org.acme.entity.Token;
import org.acme.entity.User;
import org.acme.exceptions.*;
import org.acme.model.Auth;
import org.acme.model.AuthToken;
import org.acme.utils.JwtUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDateTime;

@Slf4j
@ApplicationScoped
public class AuthService {

    @ConfigProperty(name = "jwt.expiration")
    private Long expirationDays;
    private final JWTParser jwtParser;

    @Inject
    public AuthService(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    public Uni<Auth> createUser(Auth authRequest) {
        return hashPassword(authRequest.getPassword())
                .onItem().transform(passwordHash -> {
                    User user = new User(authRequest);
                    user.setPasswordHash(passwordHash);
                    return user;
                })
                .onItem().transformToUni(this::persistUser);
    }

    private Uni<Auth> persistUser(User user) {
        return Panache.withTransaction(() -> User.persist(user))
                .onItem().transform(success -> {
                    log.info("Successfully created user, email: {}", user.getEmail());
                    return new Auth(user);
                })
                .onFailure(ConstraintViolationException.class)
                .transform(ex -> handleUserCreationError(user.getEmail()));
    }

    private Throwable handleUserCreationError(String email) {
        log.error("User already exists, email: {}", email);
        throw new AccountDoesNotExistsException(Errors.ACCOUNT_ALREADY_EXISTS + email);
    }

    private Uni<String> hashPassword(String password) {
        return Uni.createFrom().item(() -> Password.hash(password).withArgon2().getResult());
    }

    public Uni<AuthToken> authenticateUser(Auth authRequest) {
        return findByUsernameAndPassword(authRequest)
                .onItem().transformToUni(this::generateAndPersistTokens);
    }

    private Uni<AuthToken> generateTokens(User user) {
        String accessToken = JwtUtils.generateAccessToken(user, expirationDays);
        String refreshToken = JwtUtils.generateRefreshToken(user, expirationDays * 24 * 60 * 60);
        return Uni.createFrom().item(() -> new AuthToken(accessToken, refreshToken, expirationDays));
    }

    private Uni<AuthToken> generateAndPersistTokens(User user) {
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(expirationDays * 24 * 60 * 60);
        return generateTokens(user)
                .onItem().transformToUni(tokens -> persistToken(user.id, tokens.getRefreshToken(), expirationTime)
                        .onItem().transform(success -> tokens));
    }

    public Uni<User> findByUsernameAndPassword(Auth authRequest) {
        return Panache.withTransaction(() -> User.find(Entity.EMAIL, authRequest.getEmail()).firstResult())
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
        return validPassword ? handleSuccessfulLogin(user) : handleFailedLogin(user);
    }

    private Uni<Boolean> handleSuccessfulLogin(User user) {
        return persistLoginAttempt(user.id, AttemptType.SUCCESS)
                .onItem().transform(ignored -> true);
    }

    private Uni<Boolean> handleFailedLogin(User user) {
        return persistLoginAttempt(user.id, AttemptType.FAILED)
                .onItem().transform(ignored -> false);
    }

    public Uni<Void> persistLoginAttempt(Long userId, AttemptType attempt) {
        LoginAttempts loginAttempts = new LoginAttempts(userId,
                attempt.name().equalsIgnoreCase(AttemptType.SUCCESS.name()));

        return Panache.withTransaction(() -> PanacheEntityBase.persist(loginAttempts))
                .onItem().transform(success -> {
                    log.info("Persisting user login attempt data, Result: {}, userId: {}",attempt, userId);
                    return Uni.createFrom().voidItem();
                })
                .onFailure().transform(ex -> {
                    log.error("Unable to persist user login attempt, userId: {}", userId);
                    throw new UnableToPresistException(Errors.UNABLE_TO_PERSIST_LOGIN_ATTEMPT_DETAILS + ex.getMessage());
                })
                .replaceWithVoid();
    }

    public Uni<Void> persistToken(Long userId, String refreshToken, LocalDateTime expiration) {
        Token token = new Token(userId, refreshToken, expiration);
        return Panache.withTransaction(() -> PanacheEntityBase.persist(token))
                .onItem().transform(success -> {
                    log.info("Persisting refresh token details for userId: {}", userId);
                    return Uni.createFrom().voidItem();
                }).onFailure().transform(ex -> {
                    log.error("Unable to persist refresh token for userId: {}", userId);
                    throw new UnableToPresistException(Errors.UNABLE_TO_PERSIST_TOKEN_DETAILS + ex.getMessage());
                }).replaceWithVoid();
    }

    public Uni<User> findByUserId(Long userId) {
        return Panache.withTransaction(() -> User.findById(userId))
                .onItem().transform(User.class::cast)
                .onFailure().invoke(ex -> log.error("Invalid access token, user Id : {} not found", userId))
                .onFailure().transform(ex -> new InvalidAccessTokenException(Errors.INVALID_ACCESS_TOKEN));
    }


}
