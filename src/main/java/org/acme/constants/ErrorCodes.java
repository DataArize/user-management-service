package org.acme.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ErrorCodes {

    public static final String CONSTRAINT_VIOLATION_ERROR_CODE = "CONSTRAINT_VIOLATION";
    public static final String ACCOUNT_ALREADY_EXISTS = "ACCOUNT_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String PERSISTENCE_FAILED = "PERSISTENCE_FAILED";
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String EMAIL_DELIVERY_FAILED = "EMAIL_DELIVERY_FAILED";
}
