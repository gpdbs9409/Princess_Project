package com.example.princessproject.admin.dto;

import com.example.princessproject.admin.model.ScoreAdjustment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdjustmentResponse(
        Long id,
        Long userId,
        LocalDate weekStart,
        String statTypeCode,
        BigDecimal points,
        String reason,
        LocalDateTime createdAt
) {
    public static AdjustmentResponse from(ScoreAdjustment a) {
        return new AdjustmentResponse(a.getId(), a.getUserId(), a.getWeekStart(), a.getStatTypeCode(), a.getPoints(), a.getReason(), a.getCreatedAt());
    }
}
