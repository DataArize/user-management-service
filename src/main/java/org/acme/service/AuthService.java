package org.acme.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.JwtToken;
import org.acme.entity.Role;
import org.acme.entity.User;
import org.acme.exceptions.UnableToPersistException;
import org.acme.model.*;
import org.acme.utils.JwtUtils;
import org.acme.utils.PasswordUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@ApplicationScoped
public class AuthService {

    @ConfigProperty(name = "jwt.expiration")
    private Long expirationDays;
    private final UserService userService;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final PasswordResetService passwordResetService;

    @Inject
    public AuthService(UserService userService, EmailService emailService, TokenService tokenService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.emailService = emailService;
        this.tokenService = tokenService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Creates a new user account based on the provided registration request.
     *
     * <p>This method hashes the user's password and creates a new {@link User} entity from the provided
     * {@link RegistrationRequest}. The new user is then persisted in the database. If the password hashing
     * or user persistence fails, the method will return a failed {@link Uni}.</p>
     *
     * @param authRequest a {@link RegistrationRequest} object containing the user's registration details,
     *                    including email, password, first name, and last name. This parameter must be valid
     *                    according to the validation rules defined in the {@link RegistrationRequest} class.
     * @return a {@link Uni<RegistrationResponse>} that emits a {@link RegistrationResponse} object
     *         upon successful user creation, containing the user's registration details.
     *
     * @throws RuntimeException if an error occurs during password hashing or user persistence.
     *
     * @see RegistrationRequest
     * @see RegistrationResponse
     * @see User
     * @see PasswordUtils
     */
    public Uni<RegistrationResponse> createUser(RegistrationRequest authRequest) {
        return hashPassword(authRequest.getPassword())
                .onItem().transform(hashedPassword -> createUserFromAuth(hashedPassword, authRequest))
                .onItem().transformToUni(userService::persistUser);
    }

    /**
     * Creates a new {@link User} entity from the given hashed password and registration request.
     *
     * @param passwordHash the hashed password to associate with the new user.
     * @param authRequest  the {@link RegistrationRequest} containing the user's registration details.
     * @return a {@link User} entity populated with the provided registration details and hashed password.
     */
    private User createUserFromAuth(String passwordHash, RegistrationRequest authRequest) {
        User user = new User(authRequest);
        user.setPasswordHash(passwordHash);
        return user;
    }

    /**
     * Hashes the provided password using the utility method from {@link PasswordUtils}.
     *
     * @param password the plaintext password to hash.
     * @return a {@link Uni<String>} that emits the hashed password.
     *
     * @throws RuntimeException if an error occurs during the hashing process.
     */
    private Uni<String> hashPassword(String password) {
        return PasswordUtils.hashPassword(password);
    }

    /**
     * Authenticates a user based on the provided login request.
     *
     * This method verifies the user's credentials by finding the user
     * associated with the provided username and password. Upon successful
     * authentication, it generates and persists the authentication tokens
     * for the user.
     *
     * @param authRequest the login request containing the user's credentials,
     *                    including username and password.
     * @return a {@link Uni} containing an {@link AuthToken} if authentication
     *         is successful, or an error if the credentials are invalid.
     */
    public Uni<AuthToken> authenticateUser(LoginRequest authRequest) {
        return userService.findByUsernameAndPassword(authRequest)
                .onItem().transformToUni(this::generateTokensForUser);
    }

    /**
     * Generates and persists authentication tokens for the specified user.
     *
     * This method creates new authentication tokens (such as access and refresh
     * tokens) for the given user and stores them in the database.
     *
     * @param user the user for whom the tokens will be generated.
     * @return a {@link Uni} containing an {@link AuthToken} with the generated
     *         tokens.
     */
    private Uni<AuthToken> generateTokensForUser(User user) {
        return tokenService.generateAndPersistTokens(user);
    }

    /**
     * Generates a new access token using the provided refresh token details.
     *
     * This method retrieves the user associated with the given refresh token and generates a new access token
     * using the token service. The method assumes the refresh token is valid and the user exists.
     *
     * @param authToken the {@link AuthToken} containing the refresh token details.
     * @return a {@link Uni<AuthToken>} containing the newly generated access token.
     *
     */
    public Uni<AuthToken> generateNewAccessToken(AuthToken authToken) {
        return tokenService.generateAccessToken(authToken, userService::findByUserId)
                .onItem().transform(success -> success);
    }

    /**
     * Fetches the authenticated user by their user ID.
     *
     * @param userId the ID of the user to fetch
     * @return a Uni containing the authenticated user model, or an error if the user does not exist
     */
    public Uni<org.acme.model.User> fetchAuthenticatedUser(Long userId) {
        return userService.findByUserId(userId).onItem().transform(user ->
            new org.acme.model.User(user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRoles() != null ?
                            user.getRoles().stream().map(Role::getRoleName).toList()
                            : List.of(),
                    user.getStatus(),
                    user.getQuota())
        );
    }

    /**
     * Sends a password reset link to the user associated with the provided email address.
     * <p>
     * This method first attempts to find the user by the email address provided in the {@link ForgotPassword} request.
     * Once the user is found, it generates a password reset token with an expiration time, and then persists the token
     * along with the user's information in the system. After successfully persisting the token, it triggers an email
     * service to send the reset link to the user's email.
     * </p>
     *
     * @param forgotPassword an instance of {@link ForgotPassword} containing the user's email address for password reset.
     * @return a {@link Uni} containing {@code Void}. The {@link Uni} completes successfully if the password reset link
     *         is sent, or throws an exception if there is an error during the process (such as the user not being found).
     * @throws UnableToPersistException if the password reset token cannot be persisted.
     */
    public Uni<Void> sendPasswordResetLink(ForgotPassword forgotPassword) {
        return userService.findByEmail(forgotPassword).onItem()
                .transformToUni(entity -> {
                    LocalDateTime expiration = LocalDateTime.now().plusSeconds(JwtToken.PASSWORD_RESET_TOKEN_EXPIRATION);
                    String passwordResetToken = JwtUtils.generatePasswordResetToken(entity.id, JwtToken.PASSWORD_RESET_TOKEN_EXPIRATION);
                    return passwordResetService.persistPasswordResetToken(passwordResetToken, entity.id,entity.getEmail(), expiration).onItem().transform(success -> success).replaceWithVoid();
                }).replaceWithVoid();
    }

    /**
     * Resets the user's password using a provided reset token and new password.
     *
     * This method first parses the reset token to obtain the associated user ID.
     * Upon successfully parsing the token, it validates the password reset token and
     * attempts to update the user's password using the provided new password.
     * In case of a failure during any step, an error message is logged.
     *
     * @param token The reset token to validate the password reset request.
     *              This token should be a valid JWT token associated with the user.
     * @param resetPassword An object containing the new password to be set for the user.
     *                      This should include the new password as a string.
     * @return A Uni<Void> indicating the outcome of the password reset operation.
     *         It returns an empty completion when the operation is successful.
     *         If any failure occurs during token parsing or password validation,
     *         the error is logged, and the failure will propagate through the Uni.
     */
    public Uni<Void> resetUserPassword(String token, ResetPassword resetPassword) {
        return tokenService.parseRefreshToken(token).onItem()
                .transformToUni(userId -> {
                    log.info("Token Parsed successfully");
                    return passwordResetService.validatePasswordResetToken(userId, token, userService::hashPasswordAndUpdate, resetPassword.getPassword())
                            .onFailure().invoke(ex -> log.error("Password reset failed: {}", ex.getMessage()))
                            .replaceWithVoid();
                });
    }





}
