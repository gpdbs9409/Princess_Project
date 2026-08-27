package com.example.princessproject.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One member's standing for one week, for the admin weekly-refund tracker.
 *
 * successDays: 0~7 in 0.5 steps. A day uses DAILY personal missions plus that day's required
 * READING/STUDY records; cumulative WEEKLY missions are deliberately excluded so completing
 * one cannot create attendance on later/future days.
 *
 * eligible: successDays >= 6. Weekly retrospective has its own score but is not a refund gate.
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
