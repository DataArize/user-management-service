package org.acme.exceptions;

import lombok.Getter;

@Getter
public class AccountAlreadyExistsException extends RuntimeException{
    private final String errorCode;
    public AccountAlreadyExistsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
