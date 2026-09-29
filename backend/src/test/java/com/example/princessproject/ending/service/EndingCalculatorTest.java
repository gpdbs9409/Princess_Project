package com.example.princessproject.ending.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EndingCalculatorTest {

    private static Map<String, BigDecimal> week(Object... kv) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put((String) kv[i], BigDecimal.valueOf(((Number) kv[i + 1]).doubleValue()));
        return map;
    }

    private static EndingCalculator.Result calc(List<Map<String, BigDecimal>> weeks, List<EndingCalculator.Adjustment> adj, boolean mvp) {
        return EndingCalculator.calculate(new EndingCalculator.Input(
                List.of("physical", "economy", "knowledge"), weeks, adj, 27, mvp));
    }

    @Test
    void picksHighestCumulativeSelectedCapitalIgnoringCommon() {
        var r = calc(List.of(
                week("physical", 100, "economy", 50, "knowledge", 10, "common", 500),
                week("physical", 100, "economy", 50, "knowledge", 10, "common", 500)), List.of(), false);
        assertThat(r.capital()).isEqualTo("physical");
    }

    @Test
    void tieBreaksByGrowthThenLastWeekThenFixedPriority() {
        // 누적 동점(200): economy가 4주차-1주차 증가폭이 더 크다
        var growth = calc(List.of(
                week("physical", 100, "economy", 50, "knowledge", 0),
                week("physical", 100, "economy", 150, "knowledge", 0)), List.of(), false);
        assertThat(growth.capital()).isEqualTo("economy");

        // 누적·증가폭 동점 → 고정 우선순위 (심리→문화→지식→경제→신체...) 에서 지식이 앞선다
        var fixed = calc(List.of(
                week("physical", 100, "economy", 100, "knowledge", 100),
                week("physical", 100, "economy", 100, "knowledge", 100)), List.of(), false);
        assertThat(fixed.capital()).isEqualTo("knowledge");
    }

    @Test
    void score100IsTotalOverMaxPossibleAndStagesFollowThresholds() {
        // 27일 × 100 = 2700 만점. 2565 = 95.0 → S(5)
        var s = calc(List.of(week("physical", 2000, "common", 565)), List.of(), false);
        assertThat(s.score100()).isEqualByComparingTo("95.0");
        assertThat(s.finalStage()).isEqualTo(5);

        // 2560 = 94.8 → A(4) (반올림으로 등급을 올리지 않는다)
        var a = calc(List.of(week("physical", 2000, "common", 560)), List.of(), false);
        assertThat(a.finalStage()).isEqualTo(4);

        assertThat(EndingCalculator.stageFor(new BigDecimal("70"))).isEqualTo(3);
        assertThat(EndingCalculator.stageFor(new BigDecimal("55"))).isEqualTo(2);
        assertThat(EndingCalculator.stageFor(new BigDecimal("54.9"))).isEqualTo(1);
    }

    @Test
    void mvpRaisesStageByOneAndSBecomesTeaTime() {
        var d = calc(List.of(week("physical", 100)), List.of(), true);
        assertThat(d.baseStage()).isEqualTo(1);
        assertThat(d.finalStage()).isEqualTo(2);
        assertThat(d.mvpApplied()).isTrue();

        var s = calc(List.of(week("physical", 2700)), List.of(), true);
        assertThat(s.finalStage()).isEqualTo(5);
        assertThat(s.mvpApplied()).isFalse();
        assertThat(s.mvpTeaTime()).isTrue();
    }

    @Test
    void adminAdjustmentsCountTowardTotalAndCapital() {
        var r = calc(List.of(week("physical", 100, "economy", 90)),
                List.of(new EndingCalculator.Adjustment(0, "ECONOMY", BigDecimal.valueOf(20)),
                        new EndingCalculator.Adjustment(null, null, BigDecimal.valueOf(27))), false);
        assertThat(r.capital()).isEqualTo("economy");
        assertThat(r.totalScore()).isEqualByComparingTo("237");
    }

    @Test
    void scheduleSwitchesPhasesAtKstBoundaries() {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalDateTime reveal = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime close = LocalDateTime.of(2026, 10, 21, 0, 0);
        java.util.function.Function<LocalDateTime, EndingPhase> at = t -> new EndingSchedule(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 27), reveal, close,
                Clock.fixed(t.atZone(seoul).toInstant(), seoul)).phase();
        assertThat(at.apply(reveal.minusSeconds(1))).isEqualTo(EndingPhase.BEFORE_REVEAL);
        assertThat(at.apply(reveal)).isEqualTo(EndingPhase.REVEALED);
        assertThat(at.apply(close.minusSeconds(1))).isEqualTo(EndingPhase.REVEALED);
        assertThat(at.apply(close)).isEqualTo(EndingPhase.CLOSED);
    }

    @Test
    void earlyRevealOnlyForListedEnvironments() {
        assertThat(EndingSchedule.isEarlyReveal("dev", "dev")).isTrue();
        assertThat(EndingSchedule.isEarlyReveal("DEV", "dev")).isTrue();
        assertThat(EndingSchedule.isEarlyReveal("production", "dev")).isFalse();
        assertThat(EndingSchedule.isEarlyReveal("", "dev")).isFalse();
        assertThat(EndingSchedule.isEarlyReveal(null, "dev")).isFalse();
    }
}
