package com.example.princessproject.commontask.service;

/**
 * Carries a machine-readable code alongside the message so the frontend can show a
 * tailored explanation instead of a generic failure message.
 */
public class CommonTaskValidationException extends RuntimeException {

    private final String code;

    public CommonTaskValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
