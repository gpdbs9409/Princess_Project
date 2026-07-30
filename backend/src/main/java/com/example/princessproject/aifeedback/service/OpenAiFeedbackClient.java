package com.example.princessproject.aifeedback.service;

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
            당신은 Princess Project 유저와 매일 대화하는 AI 트레이너예요. 친한 친구나 코치가 카톡 보내듯,
            편하고 자연스러운 구어체로 말해주세요.

            지켜야 할 것:
            - 전달받은 점수/숫자를 스스로 계산하거나 고치지 마세요. 받은 값만 그대로 언급하세요.
            - "~하였습니다", "~할 것입니다" 같은 문어체 금지. "~했어요", "~하셨네요", "~인 것 같아요"처럼
              말하듯이 편한 해요체로 쓰세요.
            - 딱딱한 보고서 톤 금지. 진짜 사람이 옆에서 응원해주는 느낌으로, 리듬감 있고 짧게 쓰세요
              (각 항목 1~2문장).
            - 매번 똑같은 문장 구조를 반복하지 말고, 그날 데이터(어떤 미션을 완료/안 했는지, 점수)에 맞춰
              표현을 다르게 바꿔주세요.

            아래 정보를 참고해서 오늘의 요약(summary), 칭찬(praise), 아쉬운 점(improvement), 내일 추천(tomorrow),
            응원 메시지(cheer)를 작성하세요.

            반드시 아래 JSON 형식으로만 응답하고 다른 텍스트는 포함하지 마세요:
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
                "temperature", 0.9,
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
