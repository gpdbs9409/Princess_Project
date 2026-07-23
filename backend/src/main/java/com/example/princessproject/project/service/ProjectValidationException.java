package com.example.princessproject.project.service;

/**
 * Carries a machine-readable code alongside the message so the frontend can show a
 * tailored explanation instead of a generic "저장에 실패했습니다" for every failure.
 */
public class ProjectValidationException extends RuntimeException {

    private final String code;

    public ProjectValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
