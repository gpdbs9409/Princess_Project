package com.example.princessproject.aifeedback.service;

public record AiFeedbackResult(
        String summary,
        String praise,
        String improvement,
        String tomorrow,
        String cheer
) {
}
