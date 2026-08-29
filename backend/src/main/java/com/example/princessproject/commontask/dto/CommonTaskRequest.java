package com.example.princessproject.commontask.dto;

import com.example.princessproject.commontask.model.CommonTaskType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Daily READING/STUDY input. Weekly retrospectives use WeeklyRetrospectiveRequest. */
public record CommonTaskRequest(
        @NotNull CommonTaskType taskType,
        @NotNull LocalDate date,
        // READING 전용, 선택 입력 (2026-08-26 QA: 책 제목도 같이 기록하면 좋겠다는 요청 반영).
        String bookTitle,
        Integer startPage,
        Integer endPage,
        BigDecimal studyPlannedAmount,
        BigDecimal studyCompletedAmount,
        // READING/STUDY 전용, 둘 다 필수 (CommonTaskService#validateReading/validateStudy).
        String photoUrl,
        // Vision 판정은 저장 허용 여부가 아니라 운영진 확인용 true/false 플래그다.
        Boolean aiVerified,
        String memo
) {
}
