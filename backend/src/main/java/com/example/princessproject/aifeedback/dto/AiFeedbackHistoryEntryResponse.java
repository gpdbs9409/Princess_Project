package com.example.princessproject.aifeedback.dto;

import com.example.princessproject.aifeedback.model.AiFeedback;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 레오집사 채팅 화면(누적 히스토리)용 - AiFeedbackResponse와 필드는 같지만 그건 "그날 하루"
 * 하나만 담는 반면, 이건 날짜(feedbackDate)까지 포함해서 여러 날짜를 리스트로 내려줄 수 있게
 * 한다 (2026-08-26 요청: 집사 코멘트를 상단 네비바로 빼서 채팅처럼 쭉 볼 수 있게).
 */
public record AiFeedbackHistoryEntryResponse(
        Long id,
        LocalDate feedbackDate,
        LocalDateTime createdAt,
        String summary,
        String praise,
        String improvement,
        String tomorrow,
        String cheer
) {
    public static AiFeedbackHistoryEntryResponse from(AiFeedback feedback) {
        return new AiFeedbackHistoryEntryResponse(
                feedback.getId(),
                feedback.getFeedbackDate(),
                feedback.getCreatedAt(),
                feedback.getSummary(),
                feedback.getPraise(),
                feedback.getImprovement(),
                feedback.getTomorrow(),
                feedback.getCheer()
        );
    }
}
