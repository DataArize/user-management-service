package org.acme.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.constants.Query;
import org.acme.entity.User;
import org.acme.exceptions.AccountAlreadyExistsException;
import org.acme.exceptions.AccountDoesNotExistsException;
import org.acme.exceptions.InvalidLoginCredentialsException;
import org.acme.model.ForgotPassword;
import org.acme.model.LoginRequest;
import org.acme.model.RegistrationRequest;
import org.acme.model.RegistrationResponse;
import org.acme.utils.PasswordUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.jose4j.jwk.Use;

import java.util.Objects;

@Slf4j
@ApplicationScoped
public class UserService {

    private final LoginAttemptsService loginAttemptsService;

    @Inject
    public UserService(LoginAttemptsService loginAttemptsService) {
        this.loginAttemptsService = loginAttemptsService;
    }

    /**
     * Persists a new {@link User} entity to the database within a transaction.
     *
     * <p>This method attempts to save the provided {@link User} instance. If the user is successfully
     * persisted, it logs the success and returns a {@link RegistrationResponse} containing the user's details.
     * If a constraint violation occurs (such as a duplicate email), it handles the error and throws an
     * {@link AccountAlreadyExistsException}.</p>
     *
     * @param user the {@link User} entity to be persisted in the database. This entity must be valid
     *             according to the defined constraints in the database schema.
     * @return a {@link Uni<RegistrationResponse>} that emits a {@link RegistrationResponse} containing
     *         the newly created user's details upon successful persistence.
     *
     * @throws AccountAlreadyExistsException if the user already exists in the database due to a constraint violation.
     */
    public Uni<RegistrationResponse> persistUser(User user) {
        return Panache.withTransaction(user::persist)
                .onItem().transform(success -> {
                    log.info("Successfully created user, email: {}", user.getEmail());
                    return new RegistrationResponse(user);
                })
                .onFailure(ConstraintViolationException.class)
                .transform(ex -> handleUserCreationError(user.getEmail()));
    }

    /**
     * Handles errors that occur during user creation, specifically for cases where the user already exists.
     *
     * @param email the email of the user that caused the error.
     * @return a Throwable representing the error, specifically an
     *         {@link AccountAlreadyExistsException} with a detailed message.
     */
    private Throwable handleUserCreationError(String email) {
        log.error("User already exists, email: {}", email);
        return new AccountAlreadyExistsException(ExceptionMessages.ACCOUNT_ALREADY_EXISTS + email, ErrorCodes.ACCOUNT_ALREADY_EXISTS);
    }

    private Throwable handleGeneralPersistenceError(Throwable ex, String email) {
        log.error("Error occurred while creating user: {}, exception: {}",email, ex.getMessage());
        return new PersistenceException("Failed to create user", ex);
    }

    /**
     * Finds a user by their username (email) and password.
     *
     * This method retrieves a user entity based on the provided email and validates
     * the password against the stored password. If the user exists and the password
     * is valid, the user is returned. If the user does not exist, an exception is thrown.
     *
     * @param authRequest the {@link LoginRequest} containing the user's email and password.
     * @return a {@link Uni<User>} representing the user if the credentials are valid.
     * @throws AccountDoesNotExistsException if no user account is found with the provided email.
     * @throws InvalidLoginCredentialsException if the provided password is incorrect.
     */
    public Uni<User> findByUsernameAndPassword(LoginRequest authRequest) {
        return Panache.withTransaction(() -> User.find(Query.EMAIL, authRequest.getEmail()).firstResult())
                .onItem().ifNotNull()
                .transformToUni(entity -> validateUserPassword(authRequest.getPassword(), (User) entity))
                .onItem().ifNull().failWith(new AccountDoesNotExistsException(ExceptionMessages.ACCOUNT_DOES_NOT_EXISTS, ErrorCodes.ACCOUNT_NOT_FOUND));
    }

    /**
     * Validates the provided password against the user's stored password.
     *
     * This method checks if the provided password matches the user's password hash.
     * If the password is valid, it logs a successful login attempt. If invalid,
     * it logs a failed login attempt and throws an exception.
     *
     * @param password the plaintext password to validate.
     * @param user the {@link User} entity containing the user's information.
     * @return a {@link Uni<User>} representing the user if the password is valid.
     * @throws InvalidLoginCredentialsException if the provided password is incorrect.
     */
    private Uni<User> validateUserPassword(String password, User user) {
        return PasswordUtils.validatePassword(password, user,
                        loginAttemptsService::handleSuccessfulLogin,
                        loginAttemptsService::handleFailedLogin)
                .onItem().transform(valid -> {
                    if (Boolean.TRUE.equals(valid)) {
                        return user;
                    } else {
                        throw new InvalidLoginCredentialsException(ExceptionMessages.INVALID_LOGIN_CREDENTIALS, ErrorCodes.INVALID_CREDENTIALS);
                    }
                });
    }

    /**
     * Finds a user by their ID.
     *
     * This method retrieves a user entity from the database based on the provided user ID.
     * If the user does not exist, an exception is thrown.
     *
     * @param userId the ID of the user to find.
     * @return a {@link Uni<User>} representing the user if found.
     * @throws AccountDoesNotExistsException if no user account is found with the provided ID.
     */
    public Uni<User> findByUserId(Long userId) {
        return Panache.withTransaction(() -> User.findById(userId))
                .onItem().transform(user -> {
                    if(Objects.isNull(user)) throw new AccountDoesNotExistsException(ExceptionMessages.ACCOUNT_DOES_NOT_EXISTS, ErrorCodes.ACCOUNT_NOT_FOUND);
                    return (User)user;
                })
                .onFailure().transform(ex -> {
                    log.error("Account does not exists, userId : {}", userId);
                    throw new AccountDoesNotExistsException(ExceptionMessages.ACCOUNT_DOES_NOT_EXISTS, ErrorCodes.ACCOUNT_NOT_FOUND);
                });
    }

    /**
     * Finds a {@link User} by their email address provided in the {@link ForgotPassword} request.
     * <p>
     * This method searches the database for a user with the email address specified in the {@link ForgotPassword} object.
     * If the user is found, it returns the {@link User} entity. If no user is found or an error occurs, appropriate
     * exceptions are thrown.
     * </p>
     *
     * @param forgotPassword the {@link ForgotPassword} object containing the email address of the user to be found.
     * @return a {@link Uni} containing the {@link User} if found. If no user is found, an {@link AccountDoesNotExistsException} is thrown.
     * @throws AccountDoesNotExistsException if the user with the specified email does not exist in the system.
     */
    public Uni<User> findByEmail(ForgotPassword forgotPassword) {
        return Panache.withTransaction(() -> User.find(Query.EMAIL, forgotPassword.getEmail()).firstResult())
                .onItem().transform(entity -> {
                    User user = (User) entity;
                    if(Objects.isNull(user)) throw new AccountDoesNotExistsException(ExceptionMessages.ACCOUNT_DOES_NOT_EXISTS, ErrorCodes.ACCOUNT_NOT_FOUND);
                    log.info("User Details found, Email: {}", user.getEmail());
                    return user;
                }).onFailure().transform(ex -> {
                    log.error("User does not exists, Email: {}, Exception: {}", forgotPassword.getEmail(), ex.getMessage(), ex);
                    throw new AccountDoesNotExistsException(ExceptionMessages.ACCOUNT_DOES_NOT_EXISTS, ErrorCodes.ACCOUNT_NOT_FOUND);
                });
    }

    /**
     * Hashes the provided password and updates the user's password in the database.
     *
     * This method first retrieves the user entity associated with the given user ID.
     * If the user is found, it hashes the new password and then updates the user's password
     * in the database with the hashed value.
     *
     * @param userId The ID of the user whose password is to be reset. This is used to retrieve
     *               the user entity and later to update the password.
     * @param password The new password to be hashed and set for the user.
     * @return A Uni<Void> indicating the outcome of the password update operation.
     *         It completes successfully with no value if the operation is successful.
     */
    public Uni<Void> hashPasswordAndUpdate(Long userId, String password) {
        return findByUserId(userId)
                .onItem().transformToUni(entity -> {
                    log.info("Attempting to reset password, User Id: {}", userId);
                    return PasswordUtils.hashPassword(password)
                            .onItem().transformToUni(hashedPassword -> {
                                log.info("Updating password for User Id: {}", userId);
                                return updatePassword(userId, hashedPassword);
                            });
                }).replaceWithVoid();
    }

    /**
     * Updates the user's password in the database with the given hashed password.
     *
     * This method executes a database transaction to update the password for the user identified by userId.
     * It performs the update using a prepared query to ensure security and performance.
     *
     * @param userId The ID of the user whose password is to be updated.
     * @param password The new hashed password to be set for the user.
     * @return A Uni<Void> indicating the outcome of the password update operation.
     *         It completes successfully with no value if the operation is successful.
     *
     * @throws PersistenceException if there is an error during the database update operation.
     *         This indicates a problem with the database transaction, such as connection issues.
     */
    public Uni<Void> updatePassword(Long userId, String password) {
        return Panache.withTransaction(() -> User.update(Query.UPDATE_PASSWORD, password, userId)).replaceWithVoid();
    }


}
