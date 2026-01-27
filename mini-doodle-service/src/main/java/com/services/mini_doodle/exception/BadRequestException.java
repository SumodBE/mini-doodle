package com.services.mini_doodle.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Builder
public class BadRequestException extends RuntimeException{
    private final String msg;
    private final int code;
}
