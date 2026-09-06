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
            사용자가 업로드한 사진이 주어진 활동(주제)의 수행을 직접 또는 간접적으로 뒷받침하는지 판단하세요.

            판정 순서:
            1. 사진에 보이는 장소, 사물, 사람의 행동, 앱/웹 화면, 로고, 글자와 숫자를 먼저 관찰하세요.
            2. 관찰한 시각적 증거가 주어진 활동과 상식적으로 연결되는지 판단하세요.
            3. 활동 중인 순간뿐 아니라 활동 장소, 사용 도구, 진행률, 결과 또는 완료 화면도 유효한 인증으로 인정하세요.

            유효한 간접 증거의 예:
            - '계단 오르기', '계단 이용', '엘리베이터 대신 계단 이용': 계단, 계단실, 계단을 오르내리는 발이나 신체 일부
            - '언어 학습', '외국어 공부': 듀오링고 등 언어 학습 앱, 외국어 단어/문장, 학습 진행률, 연속 학습 또는 완료 화면
            - 앱 이름이나 작은 글자가 완벽하게 읽히지 않아도 화면 구성, 아이콘, 읽을 수 있는 일부 텍스트와 주변 문맥이 활동에 합리적으로 부합하면 인정

            지나치게 엄격하게 판단하지 마세요. 관련 증거가 하나 이상 있고 명백한 모순이 없다면 likelyValid를 true로 판단하세요.
            사진이 흐리거나 증거가 약하지만 관련 가능성이 있으면 true와 low confidence를 사용할 수 있습니다.
            사진이 주어진 활동과 명백히 무관하거나 활동을 뒷받침하는 증거가 전혀 없을 때만 false로 판단하세요.
            반드시 아래 JSON 형식으로만 응답하고 다른 텍스트는 포함하지 마세요.
            confidence 값은 "high", "medium", "low" 중 하나만 사용하세요.
            reason에는 사진에서 실제로 관찰한 핵심 증거와 활동의 연결 관계를 한국어 한 문장으로 설명하세요.
            예시: {"likelyValid": true, "reason": "사진에 계단이 보여 계단 이용 활동의 간접 증거로 적절합니다.", "confidence": "medium"}
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiVisionClient(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model.vision}") String model,
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
                                Map.of("type", "image_url", "image_url", Map.of(
                                        "url", dataUri,
                                        "detail", "high"
                                ))
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
