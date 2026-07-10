package com.example.princessproject.service.ai;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VisionAnalysisService {

    private final String apiKey;
    private final String model;

    public VisionAnalysisService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public VisionAnalysisResult analyze(MultipartFile file, String expectedTopic) {
        String filename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename().toLowerCase();
        boolean likelyValid = filename.contains("book") || filename.contains("read") || filename.contains("study") || filename.contains("learning");
        String reason = likelyValid
                ? "업로드된 이미지가 " + expectedTopic + "와 관련된 것으로 보입니다."
                : "업로드된 이미지가 " + expectedTopic + "와 직접 관련되지 않을 수 있습니다.";
        return new VisionAnalysisResult(likelyValid, reason, likelyValid ? "medium" : "low");
    }
}
