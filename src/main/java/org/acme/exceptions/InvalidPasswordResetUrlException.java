package org.acme.exceptions;

import lombok.Getter;

@Getter
public class InvalidPasswordResetUrlException extends RuntimeException{

    private final String errorCode;
    public InvalidPasswordResetUrlException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
