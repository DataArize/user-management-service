package org.acme.exceptions;

public class RegistrationFailedException extends RuntimeException{
    public RegistrationFailedException(String message) {
        super(message);
    }
}
