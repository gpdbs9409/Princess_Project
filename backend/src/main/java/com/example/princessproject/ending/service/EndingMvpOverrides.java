package com.example.princessproject.ending.service;

import com.example.princessproject.common.CohortNames;
import java.time.LocalDate;
import java.util.Set;

/** 2026-09-30 운영자 확인: 셩이·스텔라·세라·졍의 1기 엔딩 MVP 수기 보정. */
final class EndingMvpOverrides {
    // 운영 DB에서 확인한 불변 회원 ID. 닉네임이 바뀌어도 같은 회원에게 적용한다.
    private static final Set<Long> FIRST_COHORT_MVP_IDS = Set.of(
            49L, // 셩이
            51L, // 스텔라
            52L, // 세라
            57L  // 졍
    );

    private EndingMvpOverrides() {}

    static boolean appliesTo(Long userId, String cohort, LocalDate periodStart, LocalDate periodEnd) {
        return userId != null
                && FIRST_COHORT_MVP_IDS.contains(userId)
                && "1기".equals(CohortNames.canonical(cohort))
                && LocalDate.of(2026, 9, 1).equals(periodStart)
                && LocalDate.of(2026, 9, 27).equals(periodEnd);
    }
}
