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

    public VisionAnalysisResult analyze(MultipartFile file, String expectedTopic) {
        try {
            return visionClient.analyze(file.getBytes(), file.getContentType(), expectedTopic);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded photo", e);
        }
    }
}
