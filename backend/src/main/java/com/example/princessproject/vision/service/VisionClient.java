package com.example.princessproject.vision.service;

import com.example.princessproject.vision.dto.VisionAnalysisResult;

/**
 * The only contract AI is allowed to fulfill here: look at a photo and say whether it plausibly
 * matches the expected activity. Implementations must never decide scores or persistence - that
 * stays in DailyRecordService.
 */
public interface VisionClient {

    VisionAnalysisResult analyze(byte[] imageBytes, String contentType, String expectedTopic);
}
