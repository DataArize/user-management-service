package org.acme.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.constants.Success;
import org.acme.exceptions.*;
import org.acme.model.*;
import org.acme.service.AuthService;
import org.acme.utils.ErrorResponseUtils;

import java.util.Map;

@Slf4j
@Path("/auth")
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user in the system.
     *
     * <p>This endpoint accepts a JSON representation of the user's registration details and attempts to create a new user.
     * If the registration is successful, a 201 Created response is returned along with the created user's details.
     * If the account already exists, a 409 Conflict response is returned with an appropriate error message.</p>
     *
     * @param authRequest a {@link RegistrationRequest} object containing the user's registration details
     *                    such as email, password, first name, and last name. This parameter must be valid
     *                    according to the validation rules defined in the {@link RegistrationRequest} class.
     * @return a {@link Uni<Response>} that emits a {@link Response} indicating the outcome of the registration attempt.
     *         On success, it returns a response with status 201 Created and the details of the created user.
     *         On failure due to an existing account, it returns a response with status 409 Conflict and an error message.
     *
     * @throws AccountAlreadyExistsException if the user with the provided email already exists.
     *
     * @see RegistrationRequest
     * @see ErrorResponseUtils
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> registerUser(@Valid RegistrationRequest authRequest) {
        return authService.createUser(authRequest)
                .onItem().transform(success -> Response.status(Response.Status.CREATED)
                        .entity(success).build())
                .onFailure(AccountAlreadyExistsException.class)
                .recoverWithItem(ex -> {
                    AccountAlreadyExistsException exception = (AccountAlreadyExistsException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.REGISTRATION_FAILED, exception.getErrorCode());
                }).onFailure()
                .recoverWithItem(ex -> ErrorResponseUtils.createErrorResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, ExceptionMessages.REGISTRATION_FAILED, ErrorCodes.UNKNOWN_ERROR));
    }


    /**
     * Authenticates a user and returns an access token.
     *
     * <p>This endpoint accepts a JSON representation of the user's login credentials and attempts to authenticate the user.
     * If authentication is successful, a 200 OK response is returned along with the user's authentication details.
     * If the credentials are invalid or the account does not exist, a 409 Conflict response is returned with an appropriate error message.</p>
     *
     * @param authRequest a {@link LoginRequest} object containing the user's login credentials,
     *                    such as email and password. This parameter must be valid according to the
     *                    validation rules defined in the {@link LoginRequest} class.
     * @return a {@link Uni<Response>} that emits a {@link Response} indicating the outcome of the login attempt.
     *         On success, it returns a response with status 200 OK and the authentication details of the user.
     *         On failure due to invalid credentials or a non-existent account, it returns a response with
     *         status 409 Conflict and an error message.
     *
     * @throws InvalidLoginCredentialsException if the provided email or password is incorrect.
     * @throws AccountDoesNotExistsException if there is no account associated with the provided email.
     *
     * @see LoginRequest
     * @see ErrorResponseUtils
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> loginUser(@Valid LoginRequest authRequest) {
        return authService.authenticateUser(authRequest)
                .onItem().transform(success -> Response.status(Response.Status.OK).entity(success).build())
                .onFailure(InvalidLoginCredentialsException.class)
                .recoverWithItem(ex -> {
                    InvalidLoginCredentialsException exception = (InvalidLoginCredentialsException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.LOGIN_FAILED, exception.getErrorCode());
                }).onFailure(AccountDoesNotExistsException.class)
                .recoverWithItem(ex -> {
                    AccountDoesNotExistsException exception = (AccountDoesNotExistsException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.LOGIN_FAILED, exception.getErrorCode());
                });

    }

    /**
     * Refreshes the access token using the provided refresh token.
     *
     * This method validates the provided refresh token and generates a new access token.
     * If the refresh token is invalid or expired, an appropriate error response is returned.
     *
     * @param authToken the {@link AuthToken} containing the refresh token.
     * @return a {@link Uni<Response>} containing the new access token wrapped in an HTTP response.
     * @throws InvalidRefreshTokenException if the refresh token is invalid.
     * @throws RefreshTokenExpiredException if the refresh token has expired.
     */
    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> refreshToken(AuthToken authToken) {
        return authService.generateNewAccessToken(authToken)
                .onItem().transform(success -> Response.status(Response.Status.OK).entity(success).build())
                .onFailure(InvalidRefreshTokenException.class)
                .recoverWithItem(ex -> {
                    InvalidRefreshTokenException exception = (InvalidRefreshTokenException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.UNAUTHORIZED, ExceptionMessages.INVALID_TOKEN, exception.getErrorCode());
                })
                .onFailure(RefreshTokenExpiredException.class)
                .recoverWithItem(ex -> {
                    RefreshTokenExpiredException exception = (RefreshTokenExpiredException) ex;
                    return ErrorResponseUtils.createErrorResponse(ex.getMessage(), Response.Status.UNAUTHORIZED, ExceptionMessages.INVALID_TOKEN, exception.getErrorCode());
                })
                .onFailure().recoverWithItem(ex -> ErrorResponseUtils.createErrorResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.UNKNOWN_ERROR));

    }

    /**
     * Retrieves the currently authenticated user.
     *
     * @param securityContext the security context containing authentication information
     * @return a Uni<Response> containing the current user's information, or an error response if the user cannot be found
     */
    @GET
    @Path("/me")
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getCurrentUser(@Context SecurityContext securityContext) {
        return authService.fetchAuthenticatedUser(Long.parseLong(securityContext.getUserPrincipal().getName()))
                .onItem().transform(user -> Response.status(Response.Status.OK).entity(user).build())
                .onFailure(AccountDoesNotExistsException.class)
                .recoverWithItem(ex -> {
                    AccountDoesNotExistsException exception = (AccountDoesNotExistsException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.USER_IS_NOT_AUTHENTICATED, exception.getErrorCode());
                }).onFailure().recoverWithItem(ex -> ErrorResponseUtils.createErrorResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.UNKNOWN_ERROR));

    }


    /**
     * Handles forgot password requests by sending a password reset link to the provided email address.
     * <p>
     * This method consumes a {@link ForgotPassword} request object containing the email of the user
     * who requested the password reset. It uses the {@link AuthService} to send a password reset link.
     * If successful, it returns a 200 OK response with a message indicating that the password reset token
     * was sent. In case of any specific exceptions like {@link AccountDoesNotExistsException},
     * {@link UnableToPersistException}, or {@link EmailDeliveryException}, it handles them gracefully
     * and returns appropriate error responses.
     * <p>
     * If any other unexpected exception occurs, it returns a generic 500 Internal Server Error response.
     *
     * @param forgotPassword the {@link ForgotPassword} request object containing the email for password reset
     * @return a {@link Uni<Response>} that asynchronously sends the password reset link or returns an error response
     *
     * @throws AccountDoesNotExistsException if the account with the given email does not exist
     * @throws UnableToPersistException if there is a failure in persisting the reset token
     * @throws EmailDeliveryException if the email delivery service fails to send the reset token
     */
    @POST
    @Path("/forgot-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> forgotPassword(ForgotPassword forgotPassword) {
        return authService.sendPasswordResetLink(forgotPassword)
                .onItem().transform(success -> Response.status(Response.Status.OK)
                        .entity(Map.of(Success.MESSAGE,
                                Success.PASSWORD_RESET_TOKEN_SENT+forgotPassword.getEmail()))
                        .build())
                .onFailure(AccountDoesNotExistsException.class)
                .recoverWithItem(ex -> {
                    AccountDoesNotExistsException exception = (AccountDoesNotExistsException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.PASSWORD_RESET_FAILED, exception.getErrorCode());
                })
                .onFailure(UnableToPersistException.class)
                .recoverWithItem(ex -> {
                    UnableToPersistException exception = (UnableToPersistException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.PASSWORD_RESET_FAILED, exception.getErrorCode());
                })
                .onFailure(EmailDeliveryException.class)
                .recoverWithItem(ex -> {
                    EmailDeliveryException exception = (EmailDeliveryException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.CONFLICT, ExceptionMessages.PASSWORD_RESET_FAILED, exception.getErrorCode());
                })
                .onFailure().recoverWithItem(ex -> ErrorResponseUtils.createErrorResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.UNKNOWN_ERROR));

    }

    /**
     * Resets the user's password using a provided reset token and new password.
     *
     * This method is annotated as a POST request and expects a JSON payload containing the new password.
     * It also requires a query parameter for the reset token.
     * Upon successful password reset, it returns a response with a success message.
     * In case of an invalid token or any other failure, an appropriate error response is returned.
     *
     * @param token The reset token to validate the password reset request. This should be passed as a query parameter.
     * @param resetPassword An object containing the new password to be set for the user.
     * @return A Uni<Response> containing the response indicating the outcome of the password reset operation.
     *         On success, it returns a 200 OK status with a success message.
     *         On failure, it returns a 400 BAD REQUEST or 500 INTERNAL SERVER ERROR based on the nature of the failure.
     *
     * @throws InvalidPasswordResetUrlException if the provided token is invalid.
     *         This exception is handled within the method to return a 400 BAD REQUEST response.
     */
    @POST
    @Path("/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> resetPassword(@QueryParam("token") String token, ResetPassword resetPassword) {
        return authService.resetUserPassword(token, resetPassword)
                .onItem().transform(success ->
                    Response.status(Response.Status.OK).entity(Map.of(Success.MESSAGE, Success.PASSWORD_RESET_SUCCESSFUL)).build()
                ).onFailure().recoverWithItem(ex -> {
                    InvalidPasswordResetUrlException exception = (InvalidPasswordResetUrlException) ex;
                    return ErrorResponseUtils.createErrorResponse(exception.getMessage(), Response.Status.BAD_REQUEST, ExceptionMessages.PASSWORD_RESET_FAILED, exception.getErrorCode());
                })
                .onFailure().recoverWithItem(ex -> ErrorResponseUtils.createErrorResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.UNKNOWN_ERROR));
    }




}
