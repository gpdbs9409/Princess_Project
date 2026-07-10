package com.example.princessproject.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeeklyReportServiceTest {

    private final WeeklyReportService service = new WeeklyReportService(null);

    private WeeklyReportResult.DailyEntry entry(
            LocalDate date, double totalScore, double progress, Map<String, Double> statScores,
            List<String> completed, List<String> remaining
    ) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        statScores.forEach((k, v) -> scores.put(k, BigDecimal.valueOf(v)));
        MissionProgress progressObj = new MissionProgress(
                BigDecimal.valueOf(totalScore), BigDecimal.valueOf(progress), scores, completed, remaining);
        return new WeeklyReportResult.DailyEntry(date, progressObj);
    }

    @Test
    void sumsScoresAndAveragesProgressOverSevenDaysIncludingMissingDays() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeeklyReportResult.DailyEntry> daily = List.of(
                entry(weekStart, 20.0, 1.0, Map.of(), List.of(), List.of()),
                entry(weekStart.plusDays(1), 10.0, 0.5, Map.of(), List.of(), List.of()),
                entry(weekStart.plusDays(2), 0.0, 0.0, Map.of(), List.of(), List.of()),
                entry(weekStart.plusDays(3), 0.0, 0.0, Map.of(), List.of(), List.of()),
                entry(weekStart.plusDays(4), 0.0, 0.0, Map.of(), List.of(), List.of()),
                entry(weekStart.plusDays(5), 0.0, 0.0, Map.of(), List.of(), List.of()),
                entry(weekEnd, 0.0, 0.0, Map.of(), List.of(), List.of())
        );

        WeeklyReportResult result = service.aggregate(weekStart, weekEnd, daily);

        assertThat(result.totalScore()).isEqualByComparingTo("30.0");
        assertThat(result.averageProgress().doubleValue()).isCloseTo(1.5 / 7.0, within(0.0001));
    }

    @Test
    void sumsStatScoreTotalsAcrossDays() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeeklyReportResult.DailyEntry> daily = List.of(
                entry(weekStart, 20.0, 1.0, Map.of("physical", 20.0), List.of(), List.of()),
                entry(weekStart.plusDays(1), 20.0, 1.0, Map.of("physical", 15.0, "knowledge", 5.0), List.of(), List.of())
        );

        WeeklyReportResult result = service.aggregate(weekStart, weekEnd, daily);

        assertThat(result.statScoreTotals().get("physical")).isEqualByComparingTo("35.0");
        assertThat(result.statScoreTotals().get("knowledge")).isEqualByComparingTo("5.0");
    }

    @Test
    void countsHowManyDaysEachMissionCompleted() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WeeklyReportResult.DailyEntry> daily = List.of(
                entry(weekStart, 35.0, 1.0, Map.of(), List.of("운동", "일기"), List.of()),
                entry(weekStart.plusDays(1), 15.0, 0.5, Map.of(), List.of("일기"), List.of("운동"))
        );

        WeeklyReportResult result = service.aggregate(weekStart, weekEnd, daily);

        assertThat(result.missionCompletionCounts())
                .containsEntry("운동", 1)
                .containsEntry("일기", 2);
    }
}
