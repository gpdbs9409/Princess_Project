package com.example.princessproject.vision.service;

import com.example.princessproject.vision.dto.VisionAnalysisResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Active while no OpenAI key is configured, so the upload -> verify -> save flow can still be
 * exercised end-to-end locally without burning API calls. Always passes with low confidence
 * rather than blocking development.
 */
@Component
@ConditionalOnExpression("'${openai.api.key:}'.isEmpty()")
public class MockVisionClient implements VisionClient {

    @Override
    public VisionAnalysisResult analyze(byte[] imageBytes, String contentType, String expectedTopic) {
        boolean hasImage = imageBytes != null && imageBytes.length > 0;
        String reason = hasImage
                ? "OpenAI 키가 설정되지 않아 임시로 통과 처리되었습니다 (" + expectedTopic + ")."
                : "사진 데이터를 읽을 수 없었습니다.";
        return new VisionAnalysisResult(hasImage, reason, "low");
    }
}
