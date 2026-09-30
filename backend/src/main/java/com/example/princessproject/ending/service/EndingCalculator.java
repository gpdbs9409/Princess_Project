package com.example.princessproject.ending.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 엔딩 산정 로직 (화면설계서 v0.2 4p). DB/Spring 없이 테스트할 수 있게 순수 계산만 담는다.
 *
 *  1. 공주 유형: 본인이 고른 3자본 중 누적 점수 최고 자본 (공통 자본은 비교 대상 아님)
 *  2. 동점: ① 증가폭(4주차 − 1주차 주간 점수) ② 4주차 주간 점수 ③ 심리→문화→지식→경제→신체→언어→상징
 *  3. 총점: 누적 점수 (위클리/회고 보너스 미포함) + 어드민 보정
 *  4. 100점 환산: 총점 ÷ (산정 일수 × 하루 만점 100) × 100  (2026-09-29 혜윤 결정: 만점 대비 비율)
 *  5. 결말 단계: S 95~100 공주 / A 85~94 대공녀 / B 70~84 공녀 / C 55~69 영애 / D 0~54 각성한 조연
 *  6. MVP 성장권: 최종 등급 +1 (점수 가산 아님), 이미 S면 티타임 안내
 */
public final class EndingCalculator {

    public static final BigDecimal DAILY_MAX_POINTS = BigDecimal.valueOf(100);

    /** 동점 3차 고정 우선순위. */
    public static final List<String> FIXED_PRIORITY = List.of(
            "psychology", "culture", "knowledge", "economy", "physical", "language", "symbol");

    private EndingCalculator() {
    }

    /** weekIndex: 0-based 주차, null이면 특정 주가 아닌 전체 보정. statKey: 소문자 자본 키, null이면 총점 보정. */
    public record Adjustment(Integer weekIndex, String statKey, BigDecimal points) {
    }

    public record Input(
            List<String> selectedCapitals,
            List<Map<String, BigDecimal>> weeklyStatScores,
            List<Adjustment> adjustments,
            long periodDays,
            boolean mvp
    ) {
    }

    public record Result(
            String capital,
            int baseStage,
            int finalStage,
            BigDecimal totalScore,
            BigDecimal score100,
            boolean mvpApplied,
            boolean mvpTeaTime,
            Map<String, BigDecimal> cumulativeByCapital,
            List<Map<String, BigDecimal>> weeklyStatScores
    ) {
    }

    public static Result calculate(Input input) {
        if (input.selectedCapitals() == null || input.selectedCapitals().isEmpty()) {
            throw new IllegalArgumentException("No selected capitals");
        }
        int weekCount = input.weeklyStatScores().size();

        // 주간 점수(주차별 보정 반영)를 복사해서 만든다.
        List<Map<String, BigDecimal>> weekly = new ArrayList<>();
        for (Map<String, BigDecimal> week : input.weeklyStatScores()) {
            weekly.add(new LinkedHashMap<>(week));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, BigDecimal> week : weekly) {
            for (BigDecimal value : week.values()) {
                total = total.add(nz(value));
            }
        }

        Map<String, BigDecimal> cumulativeAdjustmentByStat = new LinkedHashMap<>();
        for (Adjustment adjustment : input.adjustments()) {
            BigDecimal points = nz(adjustment.points());
            total = total.add(points);
            if (adjustment.statKey() == null) continue;
            String key = adjustment.statKey().toLowerCase();
            Integer weekIndex = adjustment.weekIndex();
            if (weekIndex != null && weekIndex >= 0 && weekIndex < weekCount) {
                weekly.get(weekIndex).merge(key, points, BigDecimal::add);
            } else {
                cumulativeAdjustmentByStat.merge(key, points, BigDecimal::add);
            }
        }

        Map<String, BigDecimal> cumulative = new LinkedHashMap<>();
        for (String capital : input.selectedCapitals()) {
            String key = capital.toLowerCase();
            BigDecimal sum = cumulativeAdjustmentByStat.getOrDefault(key, BigDecimal.ZERO);
            for (Map<String, BigDecimal> week : weekly) {
                sum = sum.add(nz(week.get(key)));
            }
            cumulative.put(key, sum);
        }

        Comparator<String> byCumulative = Comparator.comparing(cumulative::get);
        Comparator<String> byGrowth = Comparator.comparing(key -> growth(weekly, key));
        Comparator<String> byLastWeek = Comparator.comparing(key -> lastWeek(weekly, key));
        // 고정 우선순위는 앞에 있을수록 우선이므로 인덱스가 작을수록 "크다"로 본다.
        Comparator<String> byPriority = Comparator.comparing(key -> -priorityIndex(key));
        String capital = cumulative.keySet().stream()
                .max(byCumulative.thenComparing(byGrowth).thenComparing(byLastWeek).thenComparing(byPriority))
                .orElseThrow();

        BigDecimal maxPossible = DAILY_MAX_POINTS.multiply(BigDecimal.valueOf(Math.max(1, input.periodDays())));
        BigDecimal score100 = total.multiply(BigDecimal.valueOf(100))
                .divide(maxPossible, 1, RoundingMode.HALF_UP);
        if (score100.signum() < 0) score100 = BigDecimal.ZERO.setScale(1);
        if (score100.compareTo(BigDecimal.valueOf(100)) > 0) score100 = BigDecimal.valueOf(100).setScale(1);

        int baseStage = stageFor(score100);
        boolean mvpApplied = input.mvp() && baseStage < 5;
        boolean mvpTeaTime = input.mvp() && baseStage == 5;
        int finalStage = mvpApplied ? baseStage + 1 : baseStage;

        return new Result(capital, baseStage, finalStage, total, score100, mvpApplied, mvpTeaTime,
                cumulative, weekly);
    }

    /** 1 각성한 조연(D) · 2 영애(C) · 3 공녀(B) · 4 대공녀(A) · 5 공주(S). 경계는 내림(94.9 → A). */
    public static int stageFor(BigDecimal score100) {
        if (score100.compareTo(BigDecimal.valueOf(95)) >= 0) return 5;
        if (score100.compareTo(BigDecimal.valueOf(85)) >= 0) return 4;
        if (score100.compareTo(BigDecimal.valueOf(70)) >= 0) return 3;
        if (score100.compareTo(BigDecimal.valueOf(55)) >= 0) return 2;
        return 1;
    }

    public static String gradeFor(int stage) {
        return switch (stage) {
            case 5 -> "S";
            case 4 -> "A";
            case 3 -> "B";
            case 2 -> "C";
            default -> "D";
        };
    }

    private static BigDecimal growth(List<Map<String, BigDecimal>> weekly, String key) {
        if (weekly.isEmpty()) return BigDecimal.ZERO;
        return lastWeek(weekly, key).subtract(nz(weekly.get(0).get(key)));
    }

    private static BigDecimal lastWeek(List<Map<String, BigDecimal>> weekly, String key) {
        if (weekly.isEmpty()) return BigDecimal.ZERO;
        return nz(weekly.get(weekly.size() - 1).get(key));
    }

    private static int priorityIndex(String key) {
        int index = FIXED_PRIORITY.indexOf(key);
        return index < 0 ? FIXED_PRIORITY.size() : index;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
