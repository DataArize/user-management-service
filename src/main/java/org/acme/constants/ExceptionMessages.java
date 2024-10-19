package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ExceptionMessages {

    public static final String INVALID_LOGIN_CREDENTIALS = "Email/Password is invalid";
    public static final String UNABLE_TO_PERSIST_LOGIN_ATTEMPT_DETAILS = "unable to persist login attempt details, Exception : ";
    public static final String ACCOUNT_DOES_NOT_EXISTS = "Account does not exists";
    public static final String UNABLE_TO_PERSIST_TOKEN_DETAILS = "unable to persist token details, Exception : ";
    public static final String ACCOUNT_ALREADY_EXISTS = "Account already exists for email : ";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String UNABLE_TO_PERSIST_PASSWORD_RESET_TOKEN = "Unable to persist password reset token";
    public static final String EMAIL_DELIVERY_FAILED = "Email delivery failed, email: ";
    public static final String REGISTRATION_FAILED = "Registration failed";
    public static final String CONSTRAINT_VIOLATION = "Constraint violation";
    public static final String INVALID_TOKEN = "Invalid Token";
    public static final String USER_IS_NOT_AUTHENTICATED = "User is not authenticated";
    public static final String PASSWORD_RESET_FAILED = "Password reset failed";
    public static final String INVALID_PASSWORD_RESET_URL = "Invalid password reset url";
    public static final String LOGIN_FAILED = "Login failed";
}
