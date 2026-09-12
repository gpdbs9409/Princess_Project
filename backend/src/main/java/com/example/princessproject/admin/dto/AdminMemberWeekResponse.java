package com.example.princessproject.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One member's standing for one week, for the admin weekly-refund tracker.
 *
 * successDays: 0~7 in 0.5 steps. A day uses DAILY personal missions plus that day's required
 * READING/STUDY records; cumulative WEEKLY missions are deliberately excluded so completing
 * one cannot create attendance on later/future days.
 *
 * eligible: successDays >= 6. Weekly retrospective is optional and affects neither score nor refund.
 * paid: whether an operator has actually marked the 25,000원 as sent for this week.
 */
public record AdminMemberWeekResponse(
        Long userId,
        String nickname,
        String cohort,
        LocalDate weekStart,
        LocalDate weekEnd,
        double successDays,
        List<Double> dailyCredits,
        boolean eligible,
        boolean paid,
        BigDecimal amount,
        LocalDateTime paidAt,
        boolean isMvp,
        String role,
        /**
         * "환급하기" 클릭 한 건에 대해 환급 시트 반영을 시도한 결과 (2026-09 추가). 지급 상태를
         * 저장한 요청에서만 값이 채워지고(PaybackSheetService.SheetSyncOutcome의 이름), 그 외의
         * 조회성 응답에서는 항상 null이다.
         */
        String sheetSyncStatus
) {
    public AdminMemberWeekResponse withSheetSyncStatus(String sheetSyncStatus) {
        return new AdminMemberWeekResponse(userId, nickname, cohort, weekStart, weekEnd, successDays, dailyCredits,
                eligible, paid, amount, paidAt, isMvp, role, sheetSyncStatus);
    }
}
