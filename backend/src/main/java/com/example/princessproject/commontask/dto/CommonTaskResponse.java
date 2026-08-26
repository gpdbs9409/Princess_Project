package com.example.princessproject.commontask.dto;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CommonTaskResponse(
        Long id,
        CommonTaskType taskType,
        LocalDate recordDate,
        String bookTitle,
        Integer startPage,
        Integer endPage,
        BigDecimal studyPlannedAmount,
        BigDecimal studyCompletedAmount,
        String retroDailyLife,
        String retroWeekReview,
        String retroNextWeekPlan,
        String photoUrl,
        String memo
) {
    public static CommonTaskResponse from(CommonTaskRecord record) {
        return new CommonTaskResponse(
                record.getId(),
                record.getTaskType(),
                record.getRecordDate(),
                record.getBookTitle(),
                record.getStartPage(),
                record.getEndPage(),
                record.getStudyPlannedAmount(),
                record.getStudyCompletedAmount(),
                record.getRetroDailyLife(),
                record.getRetroWeekReview(),
                record.getRetroNextWeekPlan(),
                record.getPhotoUrl(),
                record.getMemo()
        );
    }
}
