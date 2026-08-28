package com.example.princessproject.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only participant activity row for the admin challenge-history modal. */
public record AdminActivityResponse(
        Long id,
        Long userId,
        String nickname,
        String activityType,
        String name,
        LocalDate recordDate,
        BigDecimal actualValue,
        BigDecimal targetValue,
        String unit,
        BigDecimal earnedScore,
        BigDecimal achievementRate,
        String detail,
        String memo,
        String photoUrl,
        Boolean aiVerified,
        LocalDateTime recordedAt
) {
}
