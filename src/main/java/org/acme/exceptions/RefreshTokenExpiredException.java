package org.acme.exceptions;

import lombok.Getter;

@Getter
public class RefreshTokenExpiredException extends RuntimeException{
    private final String errorCode;
    public RefreshTokenExpiredException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
