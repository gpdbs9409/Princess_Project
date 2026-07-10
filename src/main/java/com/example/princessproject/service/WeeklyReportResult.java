package com.example.princessproject.service;

import com.example.princessproject.domain.StatType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReportResult(
        LocalDate weekStart,
        LocalDate weekEnd,
        double totalScore,
        double averageProgress,
        Map<StatType, Double> statScoreTotals,
        Map<String, Integer> missionCompletionCounts,
        List<DailyProgress> dailyBreakdown
) {
}
