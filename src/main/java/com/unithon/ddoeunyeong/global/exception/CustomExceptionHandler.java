package com.unithon.ddoeunyeong.global.exception;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException e){

        log.error("CustomException 발생 " ,e);
        BaseResponse<Object> response = BaseResponse.builder()
            .isSuccess(false)
            .code(e.getErrorCode().getCode())
            .message(e.getErrorCode().getMessage())
            .build();
        return new ResponseEntity<>(response,e.getErrorCode().getStatus());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleAny(Exception e, HttpServletRequest req) {
        log.error("[InternalServerError] {} {}", req.getMethod(), req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, 5000, "서버 내부 오류가 발생했습니다.", null, req);
    }

    private ResponseEntity<BaseResponse<Object>> build(HttpStatus status, int code, String message, Object data, HttpServletRequest req) {

        BaseResponse<Object> body = BaseResponse.<Object>builder()
            .isSuccess(false)
            .code(code)
            .message(message)
            .data(data)
            .build();

        return new ResponseEntity<>(body, status);
    }
}
