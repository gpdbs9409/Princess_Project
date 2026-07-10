package com.example.princessproject.web.dto;

import com.example.princessproject.service.MissionProgress;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DailySummaryResponse(
        LocalDate date,
        double totalScore,
        double progress,
        Map<String, Double> statScores,
        List<String> completedMissions,
        List<String> remainingMissions,
        AiFeedbackResponse aiFeedback
) {
    public static DailySummaryResponse from(LocalDate date, MissionProgress progress, AiFeedbackResponse aiFeedback) {
        Map<String, Double> statScores = progress.statScores().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name().toLowerCase(), Map.Entry::getValue));
        return new DailySummaryResponse(
                date,
                progress.totalScore(),
                progress.progress(),
                statScores,
                progress.completedMissions(),
                progress.remainingMissions(),
                aiFeedback
        );
    }
}
