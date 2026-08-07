package com.example.princessproject.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdjustmentRequest(
        LocalDate weekStart,
        String statTypeCode,
        BigDecimal points,
        String reason
) {
}
