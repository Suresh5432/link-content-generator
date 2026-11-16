package com.linklens.extractionservice.exception;

public class RetryableExtractionException extends RuntimeException{

    public RetryableExtractionException(String message) {
        super(message);
    }
    public RetryableExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
