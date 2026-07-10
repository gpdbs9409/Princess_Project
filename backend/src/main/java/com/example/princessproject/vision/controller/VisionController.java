package com.example.princessproject.vision.controller;

import com.example.princessproject.vision.dto.VisionAnalysisResult;
import com.example.princessproject.vision.service.VisionAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VisionController {

    private final VisionAnalysisService visionAnalysisService;

    public VisionController(VisionAnalysisService visionAnalysisService) {
        this.visionAnalysisService = visionAnalysisService;
    }

    @PostMapping("/api/vision/analyze")
    public VisionAnalysisResult analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "expectedTopic", required = false, defaultValue = "독서") String expectedTopic
    ) {
        return visionAnalysisService.analyze(file, expectedTopic);
    }
}
