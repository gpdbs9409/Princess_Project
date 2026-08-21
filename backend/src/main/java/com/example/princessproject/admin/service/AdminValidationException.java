package com.example.princessproject.admin.service;

/**
 * Carries a machine-readable code alongside the message so the admin frontend can show a
 * tailored explanation instead of a generic failure (mirrors ProjectValidationException).
 */
public class AdminValidationException extends RuntimeException {

    private final String code;

    public AdminValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
