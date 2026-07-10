package com.example.princessproject.record.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    @Test
    void partialAchievementIsProrated() {
        BigDecimal rate = scoringService.achievementRate(BigDecimal.valueOf(15), BigDecimal.valueOf(30));
        assertThat(rate).isEqualByComparingTo("0.5");

        BigDecimal score = scoringService.earnedScore(BigDecimal.valueOf(20), rate);
        assertThat(score).isEqualByComparingTo("10.00");
    }

    @Test
    void fullAchievementEarnsAllPoints() {
        BigDecimal rate = scoringService.achievementRate(BigDecimal.valueOf(30), BigDecimal.valueOf(30));
        assertThat(rate).isEqualByComparingTo("1.0000");

        BigDecimal score = scoringService.earnedScore(BigDecimal.valueOf(20), rate);
        assertThat(score).isEqualByComparingTo("20.00");
    }

    @Test
    void overachievingIsCappedAtOneHundredPercent() {
        BigDecimal rate = scoringService.achievementRate(BigDecimal.valueOf(60), BigDecimal.valueOf(30));
        assertThat(rate).isEqualByComparingTo("1.0000");

        BigDecimal score = scoringService.earnedScore(BigDecimal.valueOf(20), rate);
        assertThat(score).isEqualByComparingTo("20.00");
    }

    @Test
    void zeroTargetYieldsZeroRateInsteadOfDividingByZero() {
        BigDecimal rate = scoringService.achievementRate(BigDecimal.valueOf(10), BigDecimal.ZERO);
        assertThat(rate).isEqualByComparingTo("0");
    }
}
