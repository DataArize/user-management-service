package org.acme.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.Email;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.exceptions.EmailDeliveryException;

@Slf4j
@ApplicationScoped
public class EmailService {

    private final ReactiveMailer reactiveMailer;

    @Inject
    public EmailService(ReactiveMailer reactiveMailer) {
        this.reactiveMailer = reactiveMailer;
    }

    /**
     * Sends a password reset email to the specified email address with a generated reset link.
     * <p>
     * This method creates an email with the password reset link and sends it using the {@link ReactiveMailer}.
     * The reset link is generated using the provided reset token and the base URL configured in {@link Email#BASE_URL}.
     * </p>
     *
     * @param email the email address to which the reset email will be sent.
     * @param resetToken the token used to generate the reset link.
     * @return a {@link Uni} containing {@code Void}. The {@link Uni} completes after the email is successfully sent,
     *         or throws an {@link EmailDeliveryException} if sending the email fails.
     * @throws EmailDeliveryException if the email cannot be delivered to the recipient.
     */
    public Uni<Void> sendEmail(String email, String resetToken) {
        log.info("Attempting to send email, Email address: {}", email);
        String resetLink = Email.BASE_URL + resetToken;
        String emailBody = Email.BODY.replace(Email.RESET_LINK, resetLink);

        log.info("Final email content: Subject: {}, Body: {}", Email.SUBJECT, emailBody);

        reactiveMailer.send(
                Mail.withText(email, Email.SUBJECT, emailBody)
        ).subscribe().with(
                success -> log.info("Email successfully sent to {}", email),
                failure -> {
                    log.error("Failed to send email to {}", email, failure);
                    throw new EmailDeliveryException(ExceptionMessages.EMAIL_DELIVERY_FAILED, ErrorCodes.EMAIL_DELIVERY_FAILED);
                }
        );
        return Uni.createFrom().voidItem();
    }

}
