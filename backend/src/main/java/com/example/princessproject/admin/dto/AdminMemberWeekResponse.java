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
        List<Double> dailyCredits,
        boolean eligible,
        boolean paid,
        BigDecimal amount,
        LocalDateTime paidAt,
        boolean isMvp,
        String role,
        /** 그 주에 설정된 WEEKLY 미션 개수. 0이면 주간 미션 조건 자체가 없다. */
        int weeklyMissionTotal,
        /** 그중 주간 목표를 채운 개수. eligible이 되려면 total과 같아야 한다. */
        int weeklyMissionAchieved
) {
}
