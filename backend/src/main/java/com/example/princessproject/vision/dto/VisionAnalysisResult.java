package com.example.princessproject.vision.dto;

public record VisionAnalysisResult(boolean likelyValid, String reason, String confidence) {
}
