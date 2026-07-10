package com.example.princessproject.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.princessproject.mission.model.MissionDefinition;
import com.example.princessproject.mission.model.MissionType;
import com.example.princessproject.common.model.StatType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    private MissionDefinition mission(String name, StatType stat, int points, double target, boolean common) {
        return MissionDefinition.builder()
                .name(name)
                .missionType(MissionType.DAILY)
                .statType(stat)
                .assignedPoints(points)
                .targetValue(target)
                .unit("count")
                .common(common)
                .build();
    }

    @Test
    void partialAchievementIsProrated() {
        MissionDefinition reading = mission("독서", StatType.KNOWLEDGE, 30, 30, false);
        List<MissionEntry> entries = List.of(new MissionEntry(reading, 1.0));

        ScoringResult result = scoringService.calculate(entries, Set.of(StatType.KNOWLEDGE));

        assertThat(result.totalScore()).isCloseTo(1.0, within(0.001));
        assertThat(result.progress()).isCloseTo(1.0 / 30, within(0.0001));
    }

    @Test
    void nonFocusStatContributesToBonusOnly() {
        MissionDefinition exercise = mission("운동", StatType.PHYSICAL, 20, 1, false);
        MissionDefinition sideMission = mission("어학공부", StatType.LANGUAGE, 10, 1, false);
        List<MissionEntry> entries = List.of(
                new MissionEntry(exercise, 1.0),
                new MissionEntry(sideMission, 1.0)
        );

        ScoringResult result = scoringService.calculate(entries, Set.of(StatType.PHYSICAL));

        assertThat(result.totalScore()).isEqualTo(20.0);
        assertThat(result.bonusScore()).isEqualTo(10.0);
        assertThat(result.statScores()).containsOnly(java.util.Map.entry(StatType.PHYSICAL, 20.0));
        assertThat(result.progress()).isEqualTo(1.0);
    }

    @Test
    void commonMissionAlwaysCountsRegardlessOfFocus() {
        MissionDefinition journal = mission("일기", StatType.PSYCHOLOGY, 15, 1, true);
        List<MissionEntry> entries = List.of(new MissionEntry(journal, 1.0));

        ScoringResult result = scoringService.calculate(entries, Set.of(StatType.PHYSICAL));

        assertThat(result.totalScore()).isEqualTo(15.0);
        assertThat(result.bonusScore()).isEqualTo(0.0);
    }

    @Test
    void unsubmittedMissionScoresZeroButCountsTowardMaxPossible() {
        MissionDefinition exercise = mission("운동", StatType.PHYSICAL, 20, 1, false);
        List<MissionEntry> entries = List.of(new MissionEntry(exercise, null));

        ScoringResult result = scoringService.calculate(entries, Set.of(StatType.PHYSICAL));

        assertThat(result.totalScore()).isEqualTo(0.0);
        assertThat(result.progress()).isEqualTo(0.0);
    }

    @Test
    void overachievingIsCappedAtTargetRatio() {
        MissionDefinition exercise = mission("운동", StatType.PHYSICAL, 20, 1, false);
        List<MissionEntry> entries = List.of(new MissionEntry(exercise, 5.0));

        ScoringResult result = scoringService.calculate(entries, Set.of(StatType.PHYSICAL));

        assertThat(result.totalScore()).isEqualTo(20.0);
        assertThat(result.progress()).isEqualTo(1.0);
    }
}
