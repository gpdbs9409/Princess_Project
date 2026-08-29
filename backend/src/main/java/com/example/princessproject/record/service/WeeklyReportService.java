package com.example.princessproject.record.service;

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

        // Single query for the whole week instead of a growing-range query per day
        // (Monday..day1, Monday..day2, ...) - see DailyRecordService#getWeekDailyProgress.
        List<MissionProgress> weekProgress = dailyRecordService.getWeekDailyProgress(userId, weekStart);
        List<WeeklyReportResult.DailyEntry> dailyBreakdown = new ArrayList<>();
        LocalDate date = weekStart;
        for (MissionProgress progress : weekProgress) {
            dailyBreakdown.add(new WeeklyReportResult.DailyEntry(date, progress));
            date = date.plusDays(1);
        }

        MissionProgress weekTotal = dailyRecordService.getWeekTotalProgress(userId, weekStart);
        return aggregate(weekStart, weekEnd, weekTotal, dailyBreakdown);
    }

    /**
     * totalScore/statScoreTotals/averageProgress come from the pre-computed {@code weekTotal}
     * (see DailyRecordService#getWeekTotalProgress). 개인 미션과 독서·공부만 일 단위로 합산하며,
     * 선택 과제인 주간 회고는 총점과 완료 횟수 양쪽에서 제외한다.
     */
    WeeklyReportResult aggregate(
            LocalDate weekStart, LocalDate weekEnd, MissionProgress weekTotal, List<WeeklyReportResult.DailyEntry> dailyBreakdown
    ) {
        Map<String, Integer> missionCompletionCounts = new LinkedHashMap<>();
        for (WeeklyReportResult.DailyEntry entry : dailyBreakdown) {
            entry.progress().completedMissions().forEach(name -> missionCompletionCounts.merge(name, 1, Integer::sum));
        }

        return new WeeklyReportResult(
                weekStart, weekEnd, weekTotal.totalScore(), weekTotal.progress(),
                weekTotal.statScores(), missionCompletionCounts, dailyBreakdown);
    }
}
