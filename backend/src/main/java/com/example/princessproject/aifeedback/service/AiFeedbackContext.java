package com.example.princessproject.aifeedback.service;

import java.util.List;
import java.util.Map;

/**
 * Everything the AI is allowed to see: numbers the backend already computed, never raw
 * inputs it could reinterpret as a score.
 */
public record AiFeedbackContext(
        double totalScore,
        double progress,
        Map<String, Double> statScores,
        List<String> completedMissions,
        List<String> remainingMissions
) {
}
