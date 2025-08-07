package com.unithon.ddoeunyeong.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException e){
        BaseResponse<Object> response = BaseResponse.builder()
            .isSuccess(false)
            .code(e.getErrorCode().getCode())
            .message(e.getErrorCode().getMessage())
            .build();
        return new ResponseEntity<>(response,e.getErrorCode().getStatus());
    }

}
