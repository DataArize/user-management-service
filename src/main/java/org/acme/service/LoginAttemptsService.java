package org.acme.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.AttemptType;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.entity.LoginAttempts;
import org.acme.entity.User;
import org.acme.exceptions.UnableToPersistException;

@Slf4j
@ApplicationScoped
public class LoginAttemptsService {

    /**
     * Handles a successful login attempt for the specified user.
     *
     * This method persists a login attempt of type SUCCESS for the given user.
     *
     * @param user the user who has successfully logged in.
     * @return a {@link Uni<Boolean>} indicating the success of the operation,
     *         which will always return true for successful logins.
     */
    public Uni<Boolean> handleSuccessfulLogin(User user) {
        return persistLoginAttempt(user.id, AttemptType.SUCCESS)
                .onItem().transform(ignored -> true);
    }

    /**
     * Handles a failed login attempt for the specified user.
     *
     * This method persists a login attempt of type FAILED for the given user.
     *
     * @param user the user who has failed to log in.
     * @return a {@link Uni<Boolean>} indicating the success of the operation,
     *         which will always return false for failed logins.
     */
    public Uni<Boolean> handleFailedLogin(User user) {
        return persistLoginAttempt(user.id, AttemptType.FAILED)
                .onItem().transform(ignored -> false);
    }

    /**
     * Persists a login attempt for the specified user.
     *
     * This method records a login attempt for the given user ID and indicates
     * whether the attempt was successful or failed. If the persistence operation
     * fails, an exception is thrown.
     *
     * @param userId the ID of the user whose login attempt is being recorded.
     * @param attempt the type of the login attempt (SUCCESS or FAILED).
     * @return a {@link Uni<Void>} indicating the completion of the persistence operation.
     * @throws UnableToPersistException if there is an error during the persistence operation.
     */
    public Uni<Void> persistLoginAttempt(Long userId, AttemptType attempt) {
        LoginAttempts loginAttempts = new LoginAttempts(userId,
                attempt.name().equalsIgnoreCase(AttemptType.SUCCESS.name()));
        return Panache.withTransaction(() -> PanacheEntityBase.persist(loginAttempts))
                .onItem().transform(success -> {
                    log.info("Persisted user login attempt data, Result: {}, userId: {}",attempt, userId);
                    return Uni.createFrom().voidItem();
                })
                .onFailure().transform(ex -> {
                    log.error("Unable to persist user login attempt, userId: {}", userId);
                    throw new UnableToPersistException(ExceptionMessages.UNABLE_TO_PERSIST_LOGIN_ATTEMPT_DETAILS + ex.getMessage(), ErrorCodes.PERSISTENCE_FAILED);
                })
                .replaceWithVoid();
    }


}
