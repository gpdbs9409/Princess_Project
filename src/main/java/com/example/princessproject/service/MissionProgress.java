package com.example.princessproject.service;

import com.example.princessproject.domain.StatType;
import java.util.List;
import java.util.Map;

public record MissionProgress(
        double totalScore,
        double progress,
        Map<StatType, Double> statScores,
        List<String> completedMissions,
        List<String> remainingMissions
) {
}
