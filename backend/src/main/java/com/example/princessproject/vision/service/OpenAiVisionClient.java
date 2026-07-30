package com.example.princessproject.vision.service;

import com.example.princessproject.vision.dto.VisionAnalysisResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Activates once OPENAI_API_KEY is set. Sends the uploaded photo to GPT-4o mini vision and asks
 * only whether it plausibly matches the mission's expected topic (e.g. "독서" -> a book/reading
 * photo). Mirrors OpenAiFeedbackClient's pattern: one small, single-purpose OpenAI call whose
 * output is a strict JSON blob we parse straight into a record.
 */
@Component
@ConditionalOnExpression("!'${openai.api.key:}'.isEmpty()")
public class OpenAiVisionClient implements VisionClient {

    private static final String SYSTEM_PROMPT = """
            당신은 Princess Project의 인증 사진 검수 담당자입니다.
            사용자가 업로드한 사진이 주어진 활동(주제)과 관련 있어 보이는지 상식적인 수준에서 판단하세요.
            지나치게 엄격하게 보지 말고, 합리적으로 관련 있어 보이면 true로 판단하세요.
            반드시 아래 JSON 형식으로만 응답하고 다른 텍스트는 포함하지 마세요.
            confidence 값은 "high", "medium", "low" 중 하나만 사용하세요.
            예시: {"likelyValid": true, "reason": "사진에 책과 노트가 보여 독서 인증으로 적절합니다.", "confidence": "medium"}
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiVisionClient(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model}") String model,
            ObjectMapper objectMapper
    ) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public VisionAnalysisResult analyze(byte[] imageBytes, String contentType, String expectedTopic) {
        String mediaType = (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType;
        String dataUri = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text",
                                        "이 사진이 '" + expectedTopic + "' 활동 인증 사진으로 적절한지 판단해주세요."),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri))
                        ))
                )
        );

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        try {
            String content = response.path("choices").get(0).path("message").path("content").asString();
            return objectMapper.readValue(content, VisionAnalysisResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAI vision response", e);
        }
    }
}
