package com.example.user_service.exception;

import lombok.Getter;
import com.example.user_service.model.dto.exception.ErrorCodeEnum;

@Getter
public class UserException extends RuntimeException{
    private ErrorCodeEnum errorCode;

    public UserException(ErrorCodeEnum errorCode) {
        //cuman carry ErrorCode up ke Handler
        //di Service throw new yang ini
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
