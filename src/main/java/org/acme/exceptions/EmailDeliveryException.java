package org.acme.exceptions;

import lombok.Getter;

@Getter
public class EmailDeliveryException extends RuntimeException{
    private final String errorCode;
    public EmailDeliveryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
