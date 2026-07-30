package com.example.princessproject.vision.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.vision.dto.VisionAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class VisionAnalysisServiceTest {

    private final VisionAnalysisService service = new VisionAnalysisService(new MockVisionClient());

    @Test
    void passesThroughWhenNoRealVisionClientIsConfigured() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "book-reading.jpg",
                "image/jpeg",
                "dummy-image".getBytes()
        );

        VisionAnalysisResult result = service.analyze(file, "독서");

        assertThat(result.likelyValid()).isTrue();
        assertThat(result.reason()).contains("독서");
    }

    @Test
    void rejectsWhenImageBytesAreEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        VisionAnalysisResult result = service.analyze(file, "독서");

        assertThat(result.likelyValid()).isFalse();
    }
}
