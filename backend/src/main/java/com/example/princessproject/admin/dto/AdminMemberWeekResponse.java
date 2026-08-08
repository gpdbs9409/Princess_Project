package com.example.princessproject.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One member's standing for one week, for the admin weekly-refund tracker.
 *
 * successDays: 0~7 in 0.5 steps - a day counts as 1.0 if every active daily/weekly-so-far
 * mission was completed that day, 0.5 if some (but not all) were, 0 otherwise. This mirrors
 * the "완료=1일, 미완=0.5일" rule from the refund policy, approximated from the same
 * per-mission completion data the rest of the app already scores against.
 *
 * eligible: successDays >= 6 (주 6일 인증 성공 -> 예치금 1/4 환급 규칙).
 * paid: whether an operator has actually marked the 25,000원 as sent for this week.
 */
public record AdminMemberWeekResponse(
        Long userId,
        String nickname,
        String cohort,
        LocalDate weekStart,
        LocalDate weekEnd,
        double successDays,
        boolean eligible,
        boolean paid,
        BigDecimal amount,
        LocalDateTime paidAt,
        boolean isMvp,
        String role
) {
}
