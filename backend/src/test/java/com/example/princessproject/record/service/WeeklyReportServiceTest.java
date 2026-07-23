package com.example.princessproject.record.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeeklyReportServiceTest {

    private final WeeklyReportService service = new WeeklyReportService(null);

    private MissionProgress progress(
            double totalScore, double progressRatio, Map<String, Double> statScores,
            List<String> completed, List<String> remaining
    ) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        statScores.forEach((k, v) -> scores.put(k, BigDecimal.valueOf(v)));
        return new MissionProgress(
                BigDecimal.valueOf(totalScore), BigDecimal.valueOf(progressRatio), scores, completed, remaining, Map.of());
    }

    private WeeklyReportResult.DailyEntry entry(LocalDate date, List<String> completed, List<String> remaining) {
        return new WeeklyReportResult.DailyEntry(date, progress(0, 0, Map.of(), completed, remaining));
    }

    @Test
    void weekTotalsComeFromTheWeekTotalProgressNotFromSummingDays() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        MissionProgress weekTotal = progress(45.0, 0.75, Map.of("physical", 35.0, "knowledge", 10.0), List.of(), List.of());
        List<WeeklyReportResult.DailyEntry> daily = List.of(entry(weekStart, List.of(), List.of()));

        WeeklyReportResult result = service.aggregate(weekStart, weekEnd, weekTotal, daily);

        assertThat(result.totalScore()).isEqualByComparingTo("45.0");
        assertThat(result.averageProgress()).isEqualByComparingTo("0.75");
        assertThat(result.statScoreTotals().get("physical")).isEqualByComparingTo("35.0");
        assertThat(result.statScoreTotals().get("knowledge")).isEqualByComparingTo("10.0");
    }

    @Test
    void countsHowManyDaysEachMissionCompleted() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeeklyReportResult.DailyEntry> daily = List.of(
                entry(weekStart, List.of("운동", "일기"), List.of()),
                entry(weekStart.plusDays(1), List.of("일기"), List.of("운동"))
        );

        WeeklyReportResult result = service.aggregate(weekStart, weekEnd, progress(0, 0, Map.of(), List.of(), List.of()), daily);

        assertThat(result.missionCompletionCounts())
                .containsEntry("운동", 1)
                .containsEntry("일기", 2);
    }
}
