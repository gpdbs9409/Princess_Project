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
    /**
     * 환급용 일일 수행 상태. 목표 분량 달성률이 아니라 "기록을 남겼는가"를 본다.
     * 모든 개인·공통 미션 기록이 있으면 1, 하나 이상이면 0.5, 전혀 없으면 0이다.
     */
    public BigDecimal refundCredit() {
        int activeCount = missionDetails.size();
        if (activeCount == 0) return BigDecimal.ZERO;

        long performedCommonTasks = missionDetails.stream()
                .filter(detail -> detail.goalTypeCode().equalsIgnoreCase("common"))
                .filter(MissionProgressDetail::completed)
                .count();
        long performedCount = todayRecords.size() + performedCommonTasks;
        if (performedCount == 0) return BigDecimal.ZERO;
        return performedCount >= activeCount ? BigDecimal.ONE : BigDecimal.valueOf(0.5);
    }

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
