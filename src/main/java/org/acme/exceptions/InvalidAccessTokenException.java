package org.acme.exceptions;

public class InvalidAccessTokenException extends RuntimeException{
    public InvalidAccessTokenException(String message) {
        super(message);
    }
}
