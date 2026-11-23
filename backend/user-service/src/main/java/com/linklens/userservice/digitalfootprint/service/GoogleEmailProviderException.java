package com.linklens.userservice.digitalfootprint.service;

public class GoogleEmailProviderException extends RuntimeException {
    public GoogleEmailProviderException(String message) {
        super(message);
    }

    public GoogleEmailProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
