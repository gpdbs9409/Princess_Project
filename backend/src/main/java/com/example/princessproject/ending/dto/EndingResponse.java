package com.example.princessproject.ending.dto;

import com.example.princessproject.ending.service.EndingCalculator;
import com.example.princessproject.ending.service.EndingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 엔딩 카드 = capital(공주 유형) × stage(결말 단계 1~5) 35종 중 1장.
 * 카드 문구/이미지는 조합별 고정 콘텐츠라 프론트가 capital+stage로 매핑한다.
 */
public record EndingResponse(
        String nickname,
        /** 대문자 GoalTypeCode (예: "PHYSICAL") */
        String capital,
        /** 1 각성한 조연 · 2 영애 · 3 공녀 · 4 대공녀 · 5 공주 (MVP 반영 후) */
        int stage,
        String grade,
        int baseStage,
        BigDecimal score100,
        boolean mvpApplied,
        boolean mvpTeaTime,
        /** 그래프 계열 (선택 3자본 대문자 코드 + "common") */
        List<String> seriesKeys,
        List<Week> weeks
) {
    public record Week(int week, LocalDate start, LocalDate end, Map<String, BigDecimal> scores) {
    }

    public static EndingResponse of(
            String nickname, List<String> selectedCapitals, List<EndingService.WeekRange> ranges,
            EndingCalculator.Result result
    ) {
        List<String> seriesKeys = new ArrayList<>();
        for (String capital : selectedCapitals) seriesKeys.add(capital.toUpperCase());
        seriesKeys.add("common");

        List<Week> weeks = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            EndingService.WeekRange range = ranges.get(i);
            weeks.add(new Week(range.week(), range.start(), range.end(), result.weeklyStatScores().get(i)));
        }
        return new EndingResponse(
                nickname,
                result.capital().toUpperCase(),
                result.finalStage(),
                EndingCalculator.gradeFor(result.finalStage()),
                result.baseStage(),
                result.score100(),
                result.mvpApplied(),
                result.mvpTeaTime(),
                seriesKeys,
                weeks);
    }
}
