package com.services.mini_doodle.exception;

import com.services.mini_doodle.model.BaseApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CentralExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseApiResponse> handleDatabaseError(IllegalArgumentException e) {
        return ResponseEntity.status(500).body(BaseApiResponse.builder().build());
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<BaseApiResponse> handleException(InternalServerErrorException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseApiResponse.builder()
                        .statusCode(e.getCode())
                        .message(e.getMsg())
                .build());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseApiResponse> handleException(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseApiResponse.builder()
                        .statusCode(e.getCode())
                        .message(e.getMsg())
                .build());
    }




}
