package com.example.princessproject.vision.service;

import com.example.princessproject.common.OpenAiCallLimiter;
import com.example.princessproject.vision.dto.VisionAnalysisResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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

            유효한 간접 증거의 예 (아래 항목에 해당하면 다른 조건을 더 따지지 말고 즉시 likelyValid를 true로 판정하세요):
            - '계단 오르기', '계단 이용', '엘리베이터 대신 계단 이용': 계단이나 계단실이 사진 안에 보이기만 하면 충분합니다.
              사람이 계단을 오르는 순간이 찍히지 않아도(예: 계단 앞/위/옆에서 찍은 사진, 계단만 나온 사진) 인정하고, 실내외
              어떤 계단이든 상관없습니다. 발이나 오르내리는 동작이 꼭 보여야 한다고 요구하지 마세요.
            - '언어 학습', '외국어 공부', '영어 공부', '중국어 공부', '회화' 등 언어 종류를 불문한 모든 어학 활동: 듀오링고
              (Duolingo)를 포함한 언어 학습 앱 화면, 외국어 단어/문장/교재, 학습 진행률·스트릭(연속 학습일)·레슨 완료 화면
              중 하나라도 보이면 대상 언어가 무엇이든(영어, 중국어, 일본어 등) 무조건 인정하세요. 앱 UI 언어나 화면에 보이는
              설명이 다른 언어라는 이유로 거부하지 마세요.
            - '신상품등록' 등 상품 등록/관리 업무: 쇼핑몰·판매자 관리자(어드민) 화면을 캡처한 사진에서 "상품등록일",
              "상품수정일" 등 상품에 대한 플레인텍스트 항목이 그대로 보이면, 컴퓨터 화면을 찍거나 캡처한 사진이라는 사실만으로
              충분히 인정하세요. 상품명이나 세부 내용을 전부 읽을 수 없어도 무방합니다.
            - 앱 이름이나 작은 글자가 완벽하게 읽히지 않아도 화면 구성, 아이콘, 읽을 수 있는 일부 텍스트와 주변 문맥이 활동에 합리적으로 부합하면 인정

            지나치게 엄격하게 판단하지 마세요. 관련 증거가 하나 이상 있고 명백한 모순이 없다면 likelyValid를 true로 판단하세요.
            사진이 흐리거나 증거가 약하지만 관련 가능성이 있으면 true와 low confidence를 사용할 수 있습니다.
            사진이 주어진 활동과 명백히 무관하거나 활동을 뒷받침하는 증거가 전혀 없을 때만 false로 판단하세요.
            반드시 아래 JSON 형식으로만 응답하고 다른 텍스트는 포함하지 마세요.
            confidence 값은 "high", "medium", "low" 중 하나만 사용하세요.
            reason에는 사진에서 실제로 관찰한 핵심 증거와 활동의 연결 관계를 한국어 한 문장으로 설명하세요.
            예시: {"likelyValid": true, "reason": "사진에 계단이 보여 계단 이용 활동의 간접 증거로 적절합니다.", "confidence": "medium"}
            """;

    /**
     * 2차 판정에서만 시스템 프롬프트에 덧붙는다 (2026-09). 사진만으로 부적합 판정이 나온 뒤,
     * 사용자가 남긴 메모까지 함께 재검토할 때 쓰인다 - 예: "운동 사진을 못 찍어서 땀 흘린 셀카로
     * 대체해요" 같은 설명이 있으면 사진 자체는 애매해도 그 설명이 합리적이면 통과시켜야 한다.
     */
    private static final String NOTE_ADDENDUM = """

            추가 지침 (2차 판정): 이 사진은 1차 판정에서 사진만으로는 부적합(likelyValid=false) 판정을
            받았습니다. 아래 순서대로 다시 판단하세요.
            1단계 - 메모 자체 판단: 사용자가 남긴 메모가 (사용자 메시지에서 알려준) 활동과 관련이
              있고 말이 되는 설명인지 보세요. ("그냥요", "몰라요", 활동과 무관한 이야기처럼 설명이
              되지 않으면 이 단계에서 이미 false로 유지하세요.)
            2단계 - 메모와 사진의 정합성 판단: 메모가 활동과 관련 있다면, 이제 메모와 사진이 서로
              어우러지는지 보세요 - 사진이 메모의 설명과 명백히 모순되지 않고, 대체 증빙에 대한 납득
              가능한 설명이라면(예: "정면 사진을 깜빡해서 대신 이걸 찍었어요", "화면 캡처가 안 돼서
              손글씨로 적었어요") likelyValid를 true로 바꿔서 판정하세요.
            3단계 - 그 외(메모가 활동과 무관하거나, 사진이 메모·활동과 명백히 반대되는 내용)라면
              여전히 false로 판정하세요.
            - reason에는 메모를 근거로 판정을 바꿨다면 그 사실이 드러나게 한국어 한 문장으로 설명하세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenAiCallLimiter callLimiter;
    private final String model;

    public OpenAiVisionClient(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model.vision}") String model,
            ObjectMapper objectMapper,
            OpenAiCallLimiter callLimiter
    ) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.callLimiter = callLimiter;
        // 명시적 타임아웃 (2026-09): 기본 JDK HttpClient는 타임아웃이 없어서, OpenAI가 응답을 안
        // 주면 우리 쪽 요청 스레드가 무한정 붙잡혀 있었다. 비전 호출은 이미지 업로드 + 추론이라
        // 피드백 호출보다 오래 걸릴 수 있어 read timeout을 넉넉히(45초) 잡는다.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public VisionAnalysisResult analyze(byte[] imageBytes, String contentType, String expectedTopic, String note) {
        String mediaType = (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType;
        String dataUri = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        boolean hasNote = note != null && !note.isBlank();

        List<Object> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text",
                "이 사진이 '" + expectedTopic + "' 활동 인증 사진으로 적절한지 판단해주세요."));
        if (hasNote) {
            userContent.add(Map.of("type", "text", "text", "사용자가 남긴 메모: " + note));
        }
        userContent.add(Map.of("type", "image_url", "image_url", Map.of(
                "url", dataUri,
                "detail", "high"
        )));

        String systemPrompt = hasNote ? SYSTEM_PROMPT + NOTE_ADDENDUM : SYSTEM_PROMPT;
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                )
        );

        // 대기열(OpenAiCallLimiter)을 통해 나간다 - 동시 인증이 몰려도 OpenAI 레이트리밋에 걸려
        // 사용자에게 에러로 보이는 대신, 내부적으로 순서를 기다렸다가 나간다.
        JsonNode response = callLimiter.call(() -> restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class));

        try {
            String content = response.path("choices").get(0).path("message").path("content").asString();
            return objectMapper.readValue(content, VisionAnalysisResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAI vision response", e);
        }
    }
}
