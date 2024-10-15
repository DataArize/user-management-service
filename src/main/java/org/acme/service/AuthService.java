package org.acme.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.Errors;
import org.acme.entity.User;
import org.acme.exceptions.RegistrationFailedException;
import org.acme.model.Auth;

@Slf4j
@ApplicationScoped
public class AuthService {

    public Uni<Auth> createUser(Auth authRequest) {
        User user = new User(authRequest);
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
}
