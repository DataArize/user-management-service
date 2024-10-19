package org.acme.utils;

import com.password4j.Password;
import io.smallrye.mutiny.Uni;
import lombok.experimental.UtilityClass;
import org.acme.entity.User;

import java.util.function.Function;

@UtilityClass
public class PasswordUtils {

    /**
     * Hashes a password using the Argon2 algorithm.
     *
     * This method creates a reactive {@link Uni} that generates a hashed version
     * of the provided password. The password is hashed using the Argon2 algorithm
     * for secure storage.
     *
     * @param password the plain text password to be hashed.
     * @return         a {@link Uni} containing the hashed password as a {@link String}.
     */
    public static Uni<String> hashPassword(String password) {
        return Uni.createFrom().item(() -> Password.hash(password).withArgon2().getResult());
    }

    /**
     * Validates a password against the stored password hash for a user.
     *
     * This method creates a reactive {@link Uni} that checks whether the provided
     * password matches the hashed password of the given user using the Argon2
     * algorithm. Depending on the validation result, it either executes a specified
     * function for successful login handling or failed login handling.
     *
     * @param password              the plain text password to validate.
     * @param user                  the {@link User} whose password hash is to be checked.
     * @param handleSuccessfulLogin a function to be executed if the password validation succeeds.
     * @param handleFailedLogin     a function to be executed if the password validation fails.
     * @return                      a {@link Uni} containing a {@link Boolean} indicating the result of
     *                              the executed function (successful or failed login handling).
     */
    public static Uni<Boolean> validatePassword(String password, User user,
                                                Function<User, Uni<Boolean>> handleSuccessfulLogin,
                                                Function<User, Uni<Boolean>> handleFailedLogin) {
        return Uni.createFrom().item(() -> Password.check(password, user.getPasswordHash()).withArgon2())
                .onItem().transformToUni(valid -> Boolean.TRUE.equals(valid) ? handleSuccessfulLogin.apply(user) : handleFailedLogin.apply(user));
    }
}
