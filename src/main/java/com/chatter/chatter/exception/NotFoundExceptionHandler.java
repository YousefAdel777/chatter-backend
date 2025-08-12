package com.chatter.chatter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class NotFoundExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleException(NotFoundException exception) {
        Map<String, String> errors = exception.getErrorMessages();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }
}
