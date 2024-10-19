package org.acme.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.constants.JwtToken;
import org.acme.constants.Query;
import org.acme.entity.PasswordResets;
import org.acme.entity.User;
import org.acme.exceptions.InvalidPasswordResetUrlException;
import org.acme.exceptions.UnableToPersistException;
import org.acme.model.RegistrationResponse;
import org.acme.utils.JwtUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@ApplicationScoped
public class PasswordResetService {

    private final EmailService emailService;

    @Inject
    public PasswordResetService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Persists the password reset token for a given user and sends a password reset email.
     * <p>
     * This method creates a new {@link PasswordResets} entity containing the reset token, user ID, and expiration time,
     * and persists it within a transactional context. Once the token is successfully persisted, it triggers the
     * {@link EmailService} to send an email containing the password reset link to the provided user email.
     * </p>
     *
     * @param passwordResetToken the token generated for password reset.
     * @param userId the ID of the user for whom the password reset token is being generated.
     * @param email the email address to which the password reset link will be sent.
     * @param expiration the expiration date and time of the password reset token.
     * @return a {@link Uni} containing {@code Void}. The {@link Uni} completes successfully if the token is persisted
     *         and the email is sent, or throws an exception if any step fails.
     * @throws UnableToPersistException if the password reset token cannot be persisted to the database.
     */
    public Uni<Void> persistPasswordResetToken(String passwordResetToken, Long userId, String email, LocalDateTime expiration) {
        return Panache.withTransaction(() -> PanacheEntityBase.persist(new PasswordResets(passwordResetToken, userId, expiration)))
                .onItem().transform(success -> {
                    log.info("Persist password reset token for userId: {}", userId);
                    return emailService.sendEmail(email, passwordResetToken).replaceWithVoid();
                })
                .onFailure().transform(ex -> {
                    log.error("Something went wrong unable to persist reset token details for userId: {}, exception: {}", userId, ex.getMessage(), ex);
                    throw new UnableToPersistException(ExceptionMessages.UNABLE_TO_PERSIST_PASSWORD_RESET_TOKEN, ErrorCodes.PERSISTENCE_FAILED);
                })
                .replaceWithVoid();
    }

    /**
     * Validates the provided password reset token and updates the user's password if valid.
     *
     * This method checks if the reset token exists in the database for the given user ID.
     * If the token is found, it compares it with the provided token to ensure it matches.
     * If the tokens match, it invokes the provided function to hash the new password and update the user's password.
     *
     * @param userId The ID of the user whose password is being reset. This is used to fetch the corresponding reset token from the database.
     * @param token The reset token that needs to be validated. This token is compared against the one stored in the database.
     * @param hashPasswordAndUpdate A function that takes a user ID and a new password as parameters
     *                               and returns a Uni<Void> representing the asynchronous update operation.
     * @param resetPassword The new password to be set for the user if the token is valid.
     * @return A Uni<Void> indicating the outcome of the password reset validation and update operation.
     *         If the token is valid, it returns an empty completion.
     *         If the token is invalid, an exception is thrown.
     *
     * @throws InvalidPasswordResetUrlException if the reset token is not found in the database or does not match the provided token.
     *         This indicates that the provided token is either expired or invalid.
     */
    public Uni<Void> validatePasswordResetToken(Long userId, String token, BiFunction<Long, String, Uni<Void>> hashPasswordAndUpdate, String resetPassword) {
        return Panache.withTransaction(() -> PasswordResets.find(Query.FETCH_PASSWORD_RESET_TOKEN, userId).firstResult())
                .onItem().transformToUni(item -> {
                    if (Objects.isNull(item)) throw new InvalidPasswordResetUrlException(ExceptionMessages.INVALID_PASSWORD_RESET_URL, ErrorCodes.INVALID_TOKEN);
                    PasswordResets resets = (PasswordResets) item;
                    log.info("Fetched reset token from database, started comparing");
                    if (!JwtUtils.compareToken(resets.getResetToken(), token)) throw new InvalidPasswordResetUrlException(ExceptionMessages.INVALID_PASSWORD_RESET_URL, ErrorCodes.INVALID_TOKEN);
                    log.info("Tokens matched, attempting to update password");
                    return hashPasswordAndUpdate.apply(userId, resetPassword);
                }).onFailure().transform(ex -> {
                    log.error("Invalid password reset URL, Exception: {}", ex.getMessage());
                    throw new InvalidPasswordResetUrlException(ExceptionMessages.INVALID_PASSWORD_RESET_URL, ErrorCodes.INVALID_TOKEN);
                });
    }
}
