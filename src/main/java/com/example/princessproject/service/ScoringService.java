package com.example.princessproject.service;

import com.example.princessproject.domain.MissionDefinition;
import com.example.princessproject.domain.StatType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Pure scoring logic: no persistence, no AI. The backend is the only place scores are
 * computed - AI only ever receives the numbers this class produces.
 */
@Service
public class ScoringService {

    public ScoringResult calculate(List<MissionEntry> entries, Set<StatType> focusStats) {
        Map<StatType, Double> statScores = new EnumMap<>(StatType.class);
        double scoredTotal = 0;
        double bonusTotal = 0;
        double maxPossible = 0;

        for (MissionEntry entry : entries) {
            MissionDefinition mission = entry.mission();
            boolean isScored = mission.isCommon() || focusStats.contains(mission.getStatType());

            if (isScored) {
                maxPossible += mission.getAssignedPoints();
            }

            double ratio = achievementRatio(entry.inputValue(), mission.getTargetValue());
            double score = mission.getAssignedPoints() * ratio;

            if (isScored) {
                scoredTotal += score;
                statScores.merge(mission.getStatType(), score, Double::sum);
            } else {
                bonusTotal += score;
            }
        }

        double progress = maxPossible > 0 ? scoredTotal / maxPossible : 0;

        return new ScoringResult(statScores, scoredTotal, scoredTotal, scoredTotal, bonusTotal, scoredTotal, progress);
    }

    private double achievementRatio(Double inputValue, Double targetValue) {
        if (inputValue == null || targetValue == null || targetValue <= 0) {
            return 0;
        }
        return Math.min(inputValue / targetValue, 1.0);
    }
}
