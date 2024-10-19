package org.acme.exceptions;


import lombok.Getter;

@Getter
public class InvalidLoginCredentialsException extends RuntimeException{
    private final String errorCode;
    public InvalidLoginCredentialsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
