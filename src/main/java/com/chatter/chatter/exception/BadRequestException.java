package com.chatter.chatter.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BadRequestException extends RuntimeException {
  private final Map<String, String> errorMessages;

  public BadRequestException(String field, String message) {
    super(message);
    errorMessages = new HashMap<>();
    errorMessages.put(field, message);
  }
}
