package com.example.user_service.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCodeEnum {
    //catalog of every error in the user_service and what each HTTP status mean
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already registered");

    private HttpStatus status;
    private String message;

    ErrorCodeEnum(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
}
