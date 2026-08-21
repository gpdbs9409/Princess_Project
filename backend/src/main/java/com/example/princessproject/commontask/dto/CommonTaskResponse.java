package com.example.princessproject.commontask.dto;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CommonTaskResponse(
        Long id,
        CommonTaskType taskType,
        LocalDate recordDate,
        Integer startPage,
        Integer endPage,
        BigDecimal studyPlannedAmount,
        BigDecimal studyCompletedAmount,
        String retroDailyLife,
        String retroWeekReview,
        String retroNextWeekPlan,
        String memo
) {
    public static CommonTaskResponse from(CommonTaskRecord record) {
        return new CommonTaskResponse(
                record.getId(),
                record.getTaskType(),
                record.getRecordDate(),
                record.getStartPage(),
                record.getEndPage(),
                record.getStudyPlannedAmount(),
                record.getStudyCompletedAmount(),
                record.getRetroDailyLife(),
                record.getRetroWeekReview(),
                record.getRetroNextWeekPlan(),
                record.getMemo()
        );
    }
}
