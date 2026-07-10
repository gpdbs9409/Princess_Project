package com.example.princessproject.service;

import com.example.princessproject.domain.DailyRecord;
import com.example.princessproject.domain.DailyScore;
import com.example.princessproject.domain.DailyStatScore;
import com.example.princessproject.domain.MissionDefinition;
import com.example.princessproject.domain.StatType;
import com.example.princessproject.repository.DailyRecordRepository;
import com.example.princessproject.repository.DailyScoreRepository;
import com.example.princessproject.repository.DailyStatScoreRepository;
import com.example.princessproject.repository.MissionDefinitionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyReportService {

    private final DailyScoreRepository dailyScoreRepository;
    private final DailyStatScoreRepository dailyStatScoreRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final MissionDefinitionRepository missionDefinitionRepository;
    private final DailyRecordService dailyRecordService;

    public WeeklyReportService(
            DailyScoreRepository dailyScoreRepository,
            DailyStatScoreRepository dailyStatScoreRepository,
            DailyRecordRepository dailyRecordRepository,
            MissionDefinitionRepository missionDefinitionRepository,
            DailyRecordService dailyRecordService
    ) {
        this.dailyScoreRepository = dailyScoreRepository;
        this.dailyStatScoreRepository = dailyStatScoreRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.missionDefinitionRepository = missionDefinitionRepository;
        this.dailyRecordService = dailyRecordService;
    }

    @Transactional(readOnly = true)
    public WeeklyReportResult getWeeklyReport(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyScore> scores = dailyScoreRepository.findByUserIdAndDateBetween(userId, weekStart, weekEnd);
        List<DailyStatScore> statScores = dailyStatScoreRepository.findByUserIdAndDateBetween(userId, weekStart, weekEnd);
        List<DailyRecord> records = dailyRecordRepository.findByUserIdAndDateBetween(userId, weekStart, weekEnd);
        List<MissionDefinition> missions = missionDefinitionRepository.findAllByOrderByIdAsc();

        List<DailyProgress> dailyBreakdown = new ArrayList<>();
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            dailyBreakdown.add(new DailyProgress(date, dailyRecordService.getMissionProgress(userId, date)));
        }

        return aggregate(weekStart, weekEnd, scores, statScores, records, missions, dailyBreakdown);
    }

    /**
     * Pure aggregation, deliberately separated from the repository/service lookups above so
     * it can be unit tested with hand-built entities and no database (same style as
     * ScoringService).
     */
    WeeklyReportResult aggregate(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<DailyScore> scores,
            List<DailyStatScore> statScores,
            List<DailyRecord> records,
            List<MissionDefinition> missions,
            List<DailyProgress> dailyBreakdown
    ) {
        double totalScore = scores.stream().mapToDouble(DailyScore::getTotalScore).sum();
        double progressSum = scores.stream().mapToDouble(DailyScore::getProgressPercent).sum();
        double averageProgress = progressSum / 7.0;

        Map<StatType, Double> statScoreTotals = new EnumMap<>(StatType.class);
        for (DailyStatScore statScore : statScores) {
            statScoreTotals.merge(statScore.getStatType(), statScore.getScore(), Double::sum);
        }

        Map<String, Integer> missionCompletionCounts = new LinkedHashMap<>();
        for (MissionDefinition mission : missions) {
            missionCompletionCounts.put(mission.getName(), 0);
        }
        for (DailyRecord record : records) {
            MissionDefinition mission = record.getMission();
            boolean complete = record.getInputValue() != null
                    && mission.getTargetValue() != null
                    && record.getInputValue() >= mission.getTargetValue();
            if (complete) {
                missionCompletionCounts.merge(mission.getName(), 1, Integer::sum);
            }
        }

        return new WeeklyReportResult(weekStart, weekEnd, totalScore, averageProgress, statScoreTotals, missionCompletionCounts, dailyBreakdown);
    }
}
