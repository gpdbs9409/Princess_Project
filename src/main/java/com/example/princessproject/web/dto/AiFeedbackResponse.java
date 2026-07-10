package com.example.princessproject.web.dto;

import com.example.princessproject.service.ai.AiFeedbackResult;

public record AiFeedbackResponse(
        String summary,
        String praise,
        String improvement,
        String tomorrow,
        String cheer
) {
    public static AiFeedbackResponse from(AiFeedbackResult result) {
        if (result == null) {
            return null;
        }
        return new AiFeedbackResponse(result.summary(), result.praise(), result.improvement(), result.tomorrow(), result.cheer());
    }
}
