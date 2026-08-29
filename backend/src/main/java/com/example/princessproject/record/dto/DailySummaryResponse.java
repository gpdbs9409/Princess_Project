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
        /** 그날의 만점. 화면에서 "40 / 111점"처럼 분모를 같이 보여주기 위해 내려준다. */
        BigDecimal maxPossible,
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
                progress.maxPossible(),
                progress.progress(),
                progress.statScores(),
                progress.completedMissions(),
                progress.remainingMissions(),
                progress.todayRecords(),
                aiFeedback
        );
    }
}
