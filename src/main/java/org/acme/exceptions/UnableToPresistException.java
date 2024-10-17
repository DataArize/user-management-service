package org.acme.exceptions;

public class UnableToPresistException extends RuntimeException{
    public UnableToPresistException(String message) {
        super(message);
    }
}
