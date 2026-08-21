package com.example.princessproject.commontask.dto;

import com.example.princessproject.commontask.model.CommonTaskType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * date means "today" for READING/STUDY and "any day within the target week" for
 * WEEKLY_RETROSPECTIVE - CommonTaskService normalizes it to that week's Monday before saving,
 * so the frontend can just send "today" in every case.
 */
public record CommonTaskRequest(
        @NotNull CommonTaskType taskType,
        @NotNull LocalDate date,
        Integer startPage,
        Integer endPage,
        BigDecimal studyPlannedAmount,
        BigDecimal studyCompletedAmount,
        String retroDailyLife,
        String retroWeekReview,
        String retroNextWeekPlan,
        // READING/STUDY 전용, 둘 다 필수 (CommonTaskService#validateReading/validateStudy).
        String photoUrl,
        String memo
) {
}
