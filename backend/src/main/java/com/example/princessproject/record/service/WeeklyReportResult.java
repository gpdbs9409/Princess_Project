package com.example.princessproject.record.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReportResult(
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal totalScore,
        BigDecimal averageProgress,
        Map<String, BigDecimal> statScoreTotals,
        Map<String, Integer> missionCompletionCounts,
        List<DailyEntry> dailyBreakdown
) {
    public record DailyEntry(LocalDate date, MissionProgress progress) {
    }
}
