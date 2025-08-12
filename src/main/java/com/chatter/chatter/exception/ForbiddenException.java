package com.chatter.chatter.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
      super(message);
    }
}
