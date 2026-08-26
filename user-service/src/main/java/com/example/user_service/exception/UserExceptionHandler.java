package com.example.user_service.exception;

import com.example.user_service.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.user_service.exception.ErrorResponse;

import java.util.ArrayList;
import java.util.List;


@RestControllerAdvice
@Slf4j
public class UserExceptionHandler {
    //translates the exception into a response (ErrorResponse)
    @ExceptionHandler(UserException.class)
    ResponseEntity<?> handleException(UserException exception) {
        ErrorCodeEnum errorDetail = exception.getErrorCode();

        return ResponseEntity
                .status(errorDetail.getCode())
                .body(new ErrorResponse(errorDetail.name(), errorDetail.getMessage()));
    }

    //handler buat @Validation kalo ke langgar
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception){
        List<String> errorList = new ArrayList<>();
        //getBindingResult -> hasil semua validation yang error
        //getFieldErrors -> semua nama field yang validationnya ga pass
        for(FieldError error : exception.getBindingResult().getFieldErrors()){
            errorList.add(error.getField() + ": " + error.getDefaultMessage());
        }

        String message = String.join("; ", errorList);
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_FAILED", message));
    }

}
