package com.example.princessproject.record.service;

import com.example.princessproject.common.model.StatType;
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
