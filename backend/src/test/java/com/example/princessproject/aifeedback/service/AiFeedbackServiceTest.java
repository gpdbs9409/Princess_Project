package com.example.princessproject.aifeedback.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiFeedbackServiceTest {

    @Test
    void dividesKoreanDayIntoFourFeedbackPeriods() {
        assertThat(AiFeedbackService.timePeriod(0)).isEqualTo("DAWN_EARLY_MORNING");
        assertThat(AiFeedbackService.timePeriod(5)).isEqualTo("DAWN_EARLY_MORNING");
        assertThat(AiFeedbackService.timePeriod(6)).isEqualTo("MORNING");
        assertThat(AiFeedbackService.timePeriod(10)).isEqualTo("MORNING");
        assertThat(AiFeedbackService.timePeriod(11)).isEqualTo("MIDDAY_AFTERNOON");
        assertThat(AiFeedbackService.timePeriod(17)).isEqualTo("MIDDAY_AFTERNOON");
        assertThat(AiFeedbackService.timePeriod(18)).isEqualTo("EVENING_NIGHT");
        assertThat(AiFeedbackService.timePeriod(23)).isEqualTo("EVENING_NIGHT");
    }
}
