package com.example.princessproject.aifeedback.service;

import java.util.Comparator;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Active while no OpenAI key is configured. Still derives its text from the real context
 * so the mission-input flow can be exercised end-to-end before a key is available.
 */
@Component
@ConditionalOnExpression("'${openai.api.key:}'.isEmpty()")
public class MockAiFeedbackClient implements AiFeedbackClient {

    @Override
    public AiFeedbackResult generate(AiFeedbackContext context) {
        String strongestStat = context.capitals().entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue().achievementPercent()))
                .map(Map.Entry::getKey)
                .orElse(null);

        String summary = strongestStat != null
                ? "오늘은 %s 스탯을 중심으로 %.0f점을 쌓았어요.".formatted(strongestStat, context.totalScore())
                : "오늘의 활동을 기록했어요.";

        String praise = context.completedMissions().isEmpty()
                ? "오늘 첫 기록을 남긴 것 자체가 의미 있는 시작이에요."
                : "%s 미션을 완료한 점이 좋았습니다.".formatted(String.join(", ", context.completedMissions()));

        String improvement = context.remainingMissions().isEmpty()
                ? "오늘 목표 미션을 모두 마쳤어요. 완벽한 하루였습니다."
                : "%s 미션을 완료하면 루틴이 더 안정될 거예요.".formatted(String.join(", ", context.remainingMissions()));

        String tomorrow = context.remainingMissions().isEmpty()
                ? "내일도 같은 루틴을 이어가 보세요."
                : "내일은 %s부터 시작해보세요.".formatted(context.remainingMissions().get(0));

        String cheer = "오늘도 한 단계 성장했습니다. 계속 이어가 봅시다!";

        return new AiFeedbackResult(summary, praise, improvement, tomorrow, cheer);
    }

    @Override
    public String modelName() {
        return "mock";
    }
}
