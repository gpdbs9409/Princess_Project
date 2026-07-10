package com.example.princessproject.record.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.record.model.DailyRecord;
import com.example.princessproject.record.model.DailyScore;
import com.example.princessproject.record.model.DailyStatScore;
import com.example.princessproject.mission.model.MissionDefinition;
import com.example.princessproject.mission.model.MissionType;
import com.example.princessproject.common.model.StatType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyReportServiceTest {

    private final WeeklyReportService service = new WeeklyReportService(null, null, null, null, null);

    private MissionDefinition mission(String name, StatType stat, int points, double target) {
        return MissionDefinition.builder()
                .name(name)
                .missionType(MissionType.DAILY)
                .statType(stat)
                .assignedPoints(points)
                .targetValue(target)
                .unit("count")
                .common(false)
                .build();
    }

    private DailyScore dailyScore(LocalDate date, double totalScore, double progress) {
        DailyScore score = new DailyScore();
        score.setDate(date);
        score.setTotalScore(totalScore);
        score.setProgressPercent(progress);
        return score;
    }

    private DailyRecord record(MissionDefinition mission, double inputValue) {
        DailyRecord record = new DailyRecord();
        record.setMission(mission);
        record.setInputValue(inputValue);
        return record;
    }

    @Test
    void sumsScoresAndAveragesProgressOverSevenDaysIncludingMissingDays() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyScore> scores = List.of(
                dailyScore(weekStart, 20.0, 1.0),
                dailyScore(weekStart.plusDays(1), 10.0, 0.5)
        );

        WeeklyReportResult result = service.aggregate(weekStart, weekEnd, scores, List.of(), List.of(), List.of(), List.of());

        assertThat(result.totalScore()).isEqualTo(30.0);
        assertThat(result.averageProgress()).isCloseTo(1.5 / 7.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void sumsStatScoreTotalsAcrossDays() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        DailyStatScore day1 = new DailyStatScore(null, null, weekStart, StatType.PHYSICAL, 20.0);
        DailyStatScore day2 = new DailyStatScore(null, null, weekStart.plusDays(1), StatType.PHYSICAL, 15.0);
        DailyStatScore day2Knowledge = new DailyStatScore(null, null, weekStart.plusDays(1), StatType.KNOWLEDGE, 5.0);

        WeeklyReportResult result = service.aggregate(
                weekStart, weekEnd, List.of(), List.of(day1, day2, day2Knowledge), List.of(), List.of(), List.of());

        assertThat(result.statScoreTotals())
                .containsEntry(StatType.PHYSICAL, 35.0)
                .containsEntry(StatType.KNOWLEDGE, 5.0);
    }

    @Test
    void countsHowManyDaysEachMissionHitItsTarget() {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        LocalDate weekEnd = weekStart.plusDays(6);

        MissionDefinition exercise = mission("운동", StatType.PHYSICAL, 20, 1.0);
        MissionDefinition journal = mission("일기", StatType.PSYCHOLOGY, 15, 1.0);

        List<DailyRecord> records = List.of(
                record(exercise, 1.0),
                record(exercise, 0.5),
                record(journal, 1.0)
        );

        WeeklyReportResult result = service.aggregate(
                weekStart, weekEnd, List.of(), List.of(), records, List.of(exercise, journal), List.of());

        assertThat(result.missionCompletionCounts())
                .containsEntry("운동", 1)
                .containsEntry("일기", 1);
    }
}
