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
        Map<Long, TodayRecordEntry> todayRecords,
        List<MissionProgressDetail> missionDetails,
        /** 그날 받을 수 있었던 최대 점수. progress = totalScore / maxPossible. */
        BigDecimal maxPossible
) {
    public MissionProgress(
            BigDecimal totalScore,
            BigDecimal progress,
            Map<String, BigDecimal> statScores,
            List<String> completedMissions,
            List<String> remainingMissions,
            Map<Long, TodayRecordEntry> todayRecords,
            List<MissionProgressDetail> missionDetails
    ) {
        this(totalScore, progress, statScores, completedMissions, remainingMissions, todayRecords,
                missionDetails, BigDecimal.ZERO);
    }

    public MissionProgress(
            BigDecimal totalScore,
            BigDecimal progress,
            Map<String, BigDecimal> statScores,
            List<String> completedMissions,
            List<String> remainingMissions,
            Map<Long, TodayRecordEntry> todayRecords
    ) {
        this(totalScore, progress, statScores, completedMissions, remainingMissions, todayRecords,
                List.of(), BigDecimal.ZERO);
    }
}
