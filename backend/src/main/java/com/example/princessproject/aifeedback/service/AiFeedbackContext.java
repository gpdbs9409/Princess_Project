package com.example.princessproject.aifeedback.service;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Everything the AI is allowed to see: numbers the backend already computed, never raw
 * inputs it could reinterpret as a score.
 */
public record AiFeedbackContext(
        LocalDate date,
        LocalDateTime currentDateTimeKst,
        String timePeriod,
        int timeToneVariant,
        double totalScore,
        double overallAchievementPercent,
        Map<String, CapitalSummary> capitals,
        List<MissionSummary> missions,
        List<String> completedMissions,
        List<String> remainingMissions
) {
    public record CapitalSummary(double earnedScore, double possibleScore, double achievementPercent) {}

    public record MissionSummary(
            String name,
            String capital,
            String cycle,
            double target,
            double actual,
            double assignedPoints,
            double earnedScore,
            double achievementPercent,
            String status
    ) {}
}
