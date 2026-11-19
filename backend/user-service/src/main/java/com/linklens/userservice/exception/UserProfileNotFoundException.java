package com.linklens.userservice.exception;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(String message) {
        super("User profile not found: "+message);
    }
}
