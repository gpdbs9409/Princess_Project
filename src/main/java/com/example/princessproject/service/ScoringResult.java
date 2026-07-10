package com.example.princessproject.service;

import com.example.princessproject.domain.StatType;
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
