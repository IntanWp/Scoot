package com.example.user_service.exception;

import com.example.user_service.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.user_service.exception.ErrorResponse;


@RestControllerAdvice
@Slf4j
public class UserExceptionHandler {
    //translates the exception into a response (ErrorResponse)
    @ExceptionHandler(UserException.class)
    ResponseEntity<?> handleException(UserException exception) {
        ErrorCodeEnum errorDetail = exception.getErrorCode();

        return ResponseEntity
                .status(errorDetail.getCode())
                .body(new ErrorResponse(errorDetail.getCode().toString(), errorDetail.getMessage()));
    }

}
