package com.example.princessproject.record.dto;

import com.example.princessproject.record.service.WeeklyReportResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReportResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal totalScore,
        BigDecimal averageProgress,
        Map<String, BigDecimal> statScoreTotals,
        Map<String, Integer> missionCompletionCounts,
        List<DailySummaryResponse> dailyBreakdown
) {
    public static WeeklyReportResponse from(WeeklyReportResult result) {
        List<DailySummaryResponse> dailyBreakdown = result.dailyBreakdown().stream()
                .map(entry -> DailySummaryResponse.from(entry.date(), entry.progress(), null))
                .toList();
        return new WeeklyReportResponse(
                result.weekStart(),
                result.weekEnd(),
                result.totalScore(),
                result.averageProgress(),
                result.statScoreTotals(),
                result.missionCompletionCounts(),
                dailyBreakdown
        );
    }
}
