package org.acme.exceptions;

public class RefreshTokenExpiredException extends RuntimeException{
    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
