package com.example.princessproject.service.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Activates once OPENAI_API_KEY is set. Calls GPT-4o mini strictly to translate an
 * already-computed context into feedback text - the system prompt forbids it from
 * touching scores.
 */
@Component
@ConditionalOnExpression("!'${openai.api.key:}'.isEmpty()")
public class OpenAiFeedbackClient implements AiFeedbackClient {

    private static final String SYSTEM_PROMPT = """
            당신은 Princess Project의 AI 트레이너입니다.
            절대 점수를 계산하지 마세요.
            전달받은 점수를 수정하지 마세요.
            아래 정보를 기반으로 오늘의 피드백, 칭찬, 부족한 부분, 내일 추천 미션, 응원 메시지를 작성하세요.
            톤은 친근한 RPG 코치 느낌으로 작성하세요.
            반드시 다음 JSON 형식으로만 응답하세요:
            {"summary": "...", "praise": "...", "improvement": "...", "tomorrow": "...", "cheer": "..."}
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiFeedbackClient(
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
    public AiFeedbackResult generate(AiFeedbackContext context) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userContent(context))
                )
        );

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        try {
            String content = response.path("choices").get(0).path("message").path("content").asString();
            return objectMapper.readValue(content, AiFeedbackResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAI response", e);
        }
    }

    private String userContent(AiFeedbackContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AI feedback context", e);
        }
    }

    @Override
    public String modelName() {
        return model;
    }
}
