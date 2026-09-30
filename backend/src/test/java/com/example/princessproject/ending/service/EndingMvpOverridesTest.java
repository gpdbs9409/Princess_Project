package com.example.princessproject.ending.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EndingMvpOverridesTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 27);

    @ParameterizedTest
    @ValueSource(longs = {49, 51, 52, 57})
    void includesAllFourConfirmedMembers(long userId) {
        assertThat(EndingMvpOverrides.appliesTo(userId, "1기", START, END)).isTrue();
        assertThat(EndingMvpOverrides.appliesTo(userId, "1", START, END)).isTrue();
    }

    @Test
    void excludesOtherMembersCohortsAndPeriods() {
        assertThat(EndingMvpOverrides.appliesTo(48L, "1기", START, END)).isFalse();
        assertThat(EndingMvpOverrides.appliesTo(null, "1기", START, END)).isFalse();
        assertThat(EndingMvpOverrides.appliesTo(49L, "2기", START, END)).isFalse();
        assertThat(EndingMvpOverrides.appliesTo(49L, null, START, END)).isFalse();
        assertThat(EndingMvpOverrides.appliesTo(49L, "1기", START.plusYears(1), END.plusYears(1))).isFalse();
        assertThat(EndingMvpOverrides.appliesTo(49L, "1기", START, END.plusDays(1))).isFalse();
    }
}
