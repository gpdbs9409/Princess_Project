package com.example.princessproject.service.ai;

public record AiFeedbackResult(
        String summary,
        String praise,
        String improvement,
        String tomorrow,
        String cheer
) {
}
