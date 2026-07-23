package com.example.princessproject.record.service;

import com.example.princessproject.record.dto.TodayRecordEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MissionProgress(
        BigDecimal totalScore,
        BigDecimal progress,
        Map<String, BigDecimal> statScores,
        List<String> completedMissions,
        List<String> remainingMissions,
        Map<Long, TodayRecordEntry> todayRecords
) {
}
