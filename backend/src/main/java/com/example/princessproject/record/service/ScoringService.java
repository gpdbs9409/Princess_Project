package com.example.princessproject.record.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Pure scoring math, unit-testable without Spring/a DB. achievementRate is capped at 1.0 (100%)
 * even if the user's input exceeds the target - overachieving a mission doesn't earn extra
 * points beyond assignedPoints.
 */
@Service
public class ScoringService {

    public BigDecimal achievementRate(BigDecimal inputValue, BigDecimal targetValue) {
        if (targetValue == null || targetValue.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = inputValue.divide(targetValue, 4, RoundingMode.HALF_UP);
        return rate.min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    public BigDecimal earnedScore(BigDecimal assignedPoints, BigDecimal achievementRate) {
        return assignedPoints.multiply(achievementRate).setScale(2, RoundingMode.HALF_UP);
    }
}
