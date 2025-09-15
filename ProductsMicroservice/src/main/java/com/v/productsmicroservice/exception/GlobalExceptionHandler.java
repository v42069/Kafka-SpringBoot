package com.v.productsmicroservice.exception;

import com.v.productsmicroservice.responsestructure.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoanAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleLoanAlreadyExists(LoanAlreadyExistsException ex) {
        ErrorMessage error = ErrorMessage.builder()
                .timestamp(new Date())
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .path("/loans/create")
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGeneralException(Exception ex) {
        ErrorMessage error = ErrorMessage.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Something went wrong: " + ex.getMessage())
                .path("/loans/create")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
