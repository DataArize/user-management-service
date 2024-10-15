package org.acme.service;

import com.password4j.Password;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.Entity;
import org.acme.constants.Errors;
import org.acme.entity.User;
import org.acme.exceptions.InvalidLoginCredentialsException;
import org.acme.exceptions.RegistrationFailedException;
import org.acme.model.Auth;
import org.acme.model.AuthResponse;
import org.acme.utils.JwtUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwk.Use;

@Slf4j
@ApplicationScoped
public class AuthService {

    @ConfigProperty(name = "jwt.expiration")
    private Long expiration;

    public Uni<Auth> createUser(Auth authRequest) {
        User user = new User(authRequest);
        String passwordHash = Password.hash(authRequest.getPassword()).withArgon2().getResult();
        log.info("Password Hash: {}", passwordHash);
        user.setPasswordHash(passwordHash);
        return User.persist(user).onItem().transform(success -> {
            log.info("Successfully created user, email: {}", authRequest.getEmail());
            return authRequest;
        })
        .onFailure().invoke(ex -> {
            log.error("Unable to create User, email: {}, Exception: {}",
                    authRequest.getEmail(), ex.getMessage(), ex);
            throw new RegistrationFailedException(Errors.UNABLE_TO_REGISTER_USER + ex.getMessage());
        });
    }

    public Uni<AuthResponse> authenticateUser(Auth authRequest) {
        return findByUsernameAndPassword(authRequest).onItem().transform(user -> {
                    String accessToken = JwtUtils.generateAccessToken(user, expiration);
                    String refreshToken = JwtUtils.generateRefreshToken(user, expiration * 24 * 30);
                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setAccessToken(accessToken);
                    authResponse.setRefreshToken(refreshToken);
                    authResponse.setExpiresIn(expiration);
                    return authResponse;
                })
        .onFailure().recoverWithItem(ex -> {
            log.error("Invalid user credentials, authentication failed, email: {}, Exception: {}", authRequest.getEmail(), ex.getMessage(), ex);
            throw new InvalidLoginCredentialsException(Errors.INVALID_LOGIN_CREDENTIALS + ex.getMessage());
        });
    }

    public Uni<User> findByUsernameAndPassword(Auth authRequest) {
        return User.find(Entity.EMAIL, authRequest.getEmail()).firstResult()
                .onItem().ifNotNull().transform(entity -> {
                    User user = (User) entity;
                    boolean validPassword = Password.check(authRequest.getPassword(), user.getPasswordHash()).withArgon2();
                    if(validPassword) return user;
                    else throw new InvalidLoginCredentialsException(Errors.INVALID_LOGIN_CREDENTIALS);
                }).onItem().ifNull().failWith(new InvalidLoginCredentialsException(Errors.INVALID_LOGIN_CREDENTIALS));
    }
}
