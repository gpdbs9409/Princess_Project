package com.example.princessproject.record.service;

import com.example.princessproject.common.model.StatType;
import java.util.Map;

public record ScoringResult(
        Map<StatType, Double> statScores,
        double missionScore,
        double behaviorScore,
        double statScoreTotal,
        double bonusScore,
        double totalScore,
        double progress
) {
}
