package org.acme.exceptions;

import lombok.Getter;

@Getter
public class UnableToPersistException extends RuntimeException{
    private final String errorCode;
    public UnableToPersistException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
