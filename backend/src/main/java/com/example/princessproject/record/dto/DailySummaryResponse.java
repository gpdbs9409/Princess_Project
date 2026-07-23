package com.example.princessproject.record.dto;

import com.example.princessproject.aifeedback.dto.AiFeedbackResponse;
import com.example.princessproject.record.service.MissionProgress;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DailySummaryResponse(
        LocalDate date,
        BigDecimal totalScore,
        BigDecimal progress,
        Map<String, BigDecimal> statScores,
        List<String> completedMissions,
        List<String> remainingMissions,
        Map<Long, TodayRecordEntry> todayRecords,
        AiFeedbackResponse aiFeedback
) {
    public static DailySummaryResponse from(LocalDate date, MissionProgress progress, AiFeedbackResponse aiFeedback) {
        return new DailySummaryResponse(
                date,
                progress.totalScore(),
                progress.progress(),
                progress.statScores(),
                progress.completedMissions(),
                progress.remainingMissions(),
                progress.todayRecords(),
                aiFeedback
        );
    }
}
