package com.chatter.chatter.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class NotFoundException extends RuntimeException {
    private final Map<String, String> errorMessages;

    public NotFoundException(String field, String message) {
        super(message);
        errorMessages = new HashMap<>();
        errorMessages.put(field, message);
    }
}
