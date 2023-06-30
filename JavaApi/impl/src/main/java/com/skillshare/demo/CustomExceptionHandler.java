package com.skillshare.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class CustomExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleEntityNotFound(
            ResponseStatusException ex) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        return new ResponseEntity<>(ex.getReason(), headers, ex.getStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleBadJson(HttpMessageNotReadableException ex) {
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        return new ResponseEntity<>("Invalid JSON", headers, HttpStatus.BAD_REQUEST);
    }

}
