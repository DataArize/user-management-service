package org.acme.exceptions;

import lombok.Getter;

@Getter
public class InvalidRefreshTokenException extends RuntimeException{
    private final String errorCode;
    public InvalidRefreshTokenException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
