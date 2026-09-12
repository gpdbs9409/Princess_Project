package com.example.princessproject.record.service;

import com.example.princessproject.catalog.model.MissionType;
import java.math.BigDecimal;

/** Backend-computed facts safe to pass to the language model without asking it to calculate. */
public record MissionProgressDetail(
        String name,
        String goalTypeCode,
        MissionType missionType,
        BigDecimal targetValue,
        BigDecimal actualValue,
        BigDecimal assignedPoints,
        BigDecimal earnedScore,
        BigDecimal achievementRate,
        boolean completed,
        /**
         * targetValue/actualValue의 단위 (예: "분", "걸음", "쪽"). 2026-09 추가 - 이 값이 없어서
         * 레오집사 AI가 독서(쪽 단위)를 "권"이라고 잘못 표기하는 문제가 있었다. AI에게 숫자만
         * 주고 단위를 추측하게 하지 않기 위해 항상 명시적으로 함께 전달한다.
         */
        String unit
) {
}
