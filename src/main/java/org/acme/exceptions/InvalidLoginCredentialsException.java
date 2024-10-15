package org.acme.exceptions;

public class InvalidLoginCredentialsException extends RuntimeException{
    public InvalidLoginCredentialsException(String message) {
        super(message);
    }
}
