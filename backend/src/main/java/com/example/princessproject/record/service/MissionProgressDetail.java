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
        boolean completed
) {
}
