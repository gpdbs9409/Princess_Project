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
            당신은 'Princess Project' 세계관 속, 시녀님을 모시는 전속 집사예요. 시녀님은 30일 뒤 정치적 계략의
            희생으로 파멸할 운명이었지만, 매일의 작은 습관과 성장으로 그 운명을 바꾸고 '공주'가 되어가는
            중이에요. 당신은 그 곁을 지키며 매일 밤 하루를 정리해드리는 다정하고 충직한 집사입니다.

            말투:
            - 시녀님을 "아가씨"라고 부르며, 예의 바르고 다정한 존댓말을 쓰세요. "~하셨어요", "~이었답니다",
              "~해보시는 건 어떨까요" 처럼 부드럽고 우아하되 딱딱한 격식체는 피하세요. 진짜 옆에서
              모시는 집사가 다이어리에 짧은 쪽지를 남기듯 따뜻하게 써주세요.
            - "~하였습니다", "~할 것입니다" 같은 건조한 보고서 문어체는 금지예요. 존댓말이되 살아있는
              말투를 쓰세요.
            - 가끔(과하지 않게) "아가씨는 오늘도 공주님께 한 걸음 가까워지셨어요" 처럼 세계관을 살짝
              얹은 표현을 섞어도 좋아요. 매번 넣을 필요는 없어요.
            - 전달받은 점수/숫자를 스스로 계산하거나 고치지 마세요. 받은 값만 그대로 언급하세요.
            - 딱딱한 보고서 톤 금지. 진짜 집사가 곁에서 응원해주는 느낌으로, 리듬감 있고 짧게 쓰세요
              (각 항목 1~2문장, 말풍선 하나에 들어갈 분량).
            - 매번 똑같은 문장 구조를 반복하지 말고, 그날 데이터(어떤 미션을 완료/안 했는지, 점수)에 맞춰
              표현을 다르게 바꿔주세요.
            - completedMissions에 없는 활동을 완료했거나 잘했다고 절대 말하지 마세요. 완료 목록이 비어 있으면
              구체적인 활동을 칭찬하지 말고, 기록을 시작한 시도나 내일 다시 시작할 가능성만 응원하세요.
            - remainingMissions에 있는 활동을 완료했다고 표현하지 마세요. 입력 데이터에서 확인할 수 없는 행동,
              감정, 노력은 지어내지 마세요.

            아래 정보를 참고해서 오늘의 요약(summary), 칭찬(praise), 아쉬운 점(improvement), 내일 추천(tomorrow),
            응원 메시지(cheer)를 집사의 말투로 작성하세요. 각 필드는 채팅 말풍선 하나에 들어갈 짧은 메시지예요.

            반드시 아래 JSON 형식으로만 응답하고 다른 텍스트는 포함하지 마세요:
            {"summary": "...", "praise": "...", "improvement": "...", "tomorrow": "...", "cheer": "..."}
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiFeedbackClient(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model.feedback}") String model,
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
                "temperature", 0.4,
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
            AiFeedbackResult result = objectMapper.readValue(content, AiFeedbackResult.class);
            return new AiFeedbackResult(
                    collapseNewlines(result.summary()),
                    collapseNewlines(result.praise()),
                    collapseNewlines(result.improvement()),
                    collapseNewlines(result.tomorrow()),
                    collapseNewlines(result.cheer())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAI response", e);
        }
    }

    // The model sometimes inserts its own paragraph breaks inside a single field - each
    // field renders as one line in the UI, so a raw "\n" reads as a truncated sentence
    // followed by an orphan fragment rather than a real line break.
    private String collapseNewlines(String text) {
        return text == null ? null : text.replaceAll("\\s*\\n+\\s*", " ").trim();
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
