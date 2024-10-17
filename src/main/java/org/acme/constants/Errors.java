package org.acme.constants;

public final class Errors {
    private Errors() {}

    public static final String UNABLE_TO_REGISTER_USER = "unable to create user, Exception: ";
    public static final String INVALID_LOGIN_CREDENTIALS = "Email/Password is invalid";
    public static final String UNABLE_TO_PERSIST_LOGIN_ATTEMPT_DETAILS = "unable to persist login attempt details, Exception : ";
    public static final String ACCOUNT_DOES_NOT_EXISTS = "Account does not exists";
    public static final String UNABLE_TO_PERSIST_TOKEN_DETAILS = "unable to persist token details, Exception : ";
    public static final String ACCOUNT_ALREADY_EXISTS = "Account already exists for email : ";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String INVALID_ACCESS_TOKEN = "Invalid access token";
    public static final String USER_IS_NOT_AUTHENTICATED = "user is not authenticated";
}
