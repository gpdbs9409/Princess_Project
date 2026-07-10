package com.example.princessproject.web.dto;

import com.example.princessproject.service.WeeklyReportResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record WeeklyReportResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        double totalScore,
        double averageProgress,
        Map<String, Double> statScoreTotals,
        Map<String, Integer> missionCompletionCounts,
        List<DailySummaryResponse> dailyBreakdown
) {
    public static WeeklyReportResponse from(WeeklyReportResult result) {
        Map<String, Double> statScoreTotals = result.statScoreTotals().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name().toLowerCase(), Map.Entry::getValue));

        List<DailySummaryResponse> dailyBreakdown = result.dailyBreakdown().stream()
                .map(day -> DailySummaryResponse.from(day.date(), day.progress(), null))
                .toList();

        return new WeeklyReportResponse(
                result.weekStart(),
                result.weekEnd(),
                result.totalScore(),
                result.averageProgress(),
                statScoreTotals,
                result.missionCompletionCounts(),
                dailyBreakdown
        );
    }
}
