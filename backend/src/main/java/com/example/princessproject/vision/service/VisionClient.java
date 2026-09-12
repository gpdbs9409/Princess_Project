package com.example.princessproject.vision.service;

import com.example.princessproject.vision.dto.VisionAnalysisResult;

/**
 * The only contract AI is allowed to fulfill here: look at a photo and say whether it plausibly
 * matches the expected activity. Implementations must never decide scores or persistence - that
 * stays in DailyRecordService.
 */
public interface VisionClient {

    /**
     * note가 null/blank면 사진만으로 판단한다. note가 있으면(2차 판정 - 사진만으로는 부적합으로
     * 나온 뒤 사용자가 남긴 메모까지 함께 보는 경우) 그 설명도 함께 근거로 참고해서 판단한다.
     */
    VisionAnalysisResult analyze(byte[] imageBytes, String contentType, String expectedTopic, String note);
}
