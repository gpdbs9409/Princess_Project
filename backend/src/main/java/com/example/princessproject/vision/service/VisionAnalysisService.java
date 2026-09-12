package com.example.princessproject.vision.service;

import com.example.princessproject.vision.dto.VisionAnalysisResult;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VisionAnalysisService {

    private final VisionClient visionClient;

    public VisionAnalysisService(VisionClient visionClient) {
        this.visionClient = visionClient;
    }

    /**
     * 2단계 판정 (2026-09): 먼저 사진만으로 판단하고, 그 결과가 부적합(false)인데 사용자가 메모를
     * 남겨뒀다면 그 메모까지 함께 다시 한 번 판단한다. 사진만으로는 애매한 인증(예: 운동 사진 대신
     * 땀 흘린 셀카)도 사용자 설명이 합리적이면 통과시키기 위함이다. 메모가 없거나 1차 판정이 이미
     * 유효(true)면 두 번째 호출은 하지 않는다 - 불필요한 OpenAI 호출을 늘리지 않기 위해서다.
     */
    public VisionAnalysisResult analyze(MultipartFile file, String expectedTopic, String note) {
        try {
            byte[] imageBytes = file.getBytes();
            String contentType = file.getContentType();
            VisionAnalysisResult firstPass = visionClient.analyze(imageBytes, contentType, expectedTopic, null);
            boolean hasNote = note != null && !note.isBlank();
            if (firstPass.likelyValid() || !hasNote) {
                return firstPass;
            }
            return visionClient.analyze(imageBytes, contentType, expectedTopic, note);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded photo", e);
        }
    }
}
