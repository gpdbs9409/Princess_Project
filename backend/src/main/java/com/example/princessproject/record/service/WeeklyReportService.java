package com.example.princessproject.record.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyReportService {

    private static final int WEEK_LENGTH_DAYS = 7;

    private final DailyRecordService dailyRecordService;

    public WeeklyReportService(DailyRecordService dailyRecordService) {
        this.dailyRecordService = dailyRecordService;
    }

    @Transactional(readOnly = true)
    public WeeklyReportResult getWeeklyReport(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(WEEK_LENGTH_DAYS - 1L);

        List<WeeklyReportResult.DailyEntry> dailyBreakdown = new ArrayList<>();
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
            dailyBreakdown.add(new WeeklyReportResult.DailyEntry(date, progress));
        }

        return aggregate(weekStart, weekEnd, dailyBreakdown);
    }

    WeeklyReportResult aggregate(LocalDate weekStart, LocalDate weekEnd, List<WeeklyReportResult.DailyEntry> dailyBreakdown) {
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal progressSum = BigDecimal.ZERO;
        Map<String, BigDecimal> statScoreTotals = new LinkedHashMap<>();
        Map<String, Integer> missionCompletionCounts = new LinkedHashMap<>();

        for (WeeklyReportResult.DailyEntry entry : dailyBreakdown) {
            MissionProgress progress = entry.progress();
            totalScore = totalScore.add(progress.totalScore());
            progressSum = progressSum.add(progress.progress());

            progress.statScores().forEach((stat, score) -> statScoreTotals.merge(stat, score, BigDecimal::add));
            progress.completedMissions().forEach(name -> missionCompletionCounts.merge(name, 1, Integer::sum));
        }

        BigDecimal averageProgress = progressSum.divide(
                BigDecimal.valueOf(WEEK_LENGTH_DAYS), 4, RoundingMode.HALF_UP);

        return new WeeklyReportResult(
                weekStart, weekEnd, totalScore, averageProgress, statScoreTotals, missionCompletionCounts, dailyBreakdown);
    }
}
