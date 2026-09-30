package com.example.princessproject.ending.service;

import lombok.Getter;

/**
 * ENDING_NOT_REVEALED (403) - 공개 전 조회
 * ENDING_NOT_AVAILABLE (404) - 선택한 자본이 없어 산정 대상이 아님
 */
@Getter
public class EndingException extends RuntimeException {
    private final String code;
    private final int status;

    public EndingException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
