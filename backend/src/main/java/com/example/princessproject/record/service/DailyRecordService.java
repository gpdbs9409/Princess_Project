package com.example.princessproject.record.service;

import com.example.princessproject.record.model.DailyRecord;
import com.example.princessproject.record.model.DailyScore;
import com.example.princessproject.record.model.DailyStatScore;
import com.example.princessproject.mission.model.MissionDefinition;
import com.example.princessproject.common.model.StatType;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.model.UserStatFocus;
import com.example.princessproject.record.repository.DailyRecordRepository;
import com.example.princessproject.record.repository.DailyScoreRepository;
import com.example.princessproject.record.repository.DailyStatScoreRepository;
import com.example.princessproject.mission.repository.MissionDefinitionRepository;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the write path (save a mission record, recompute scores) and the read path
 * (fetch today's progress) so both agree on how mission catalog + records are combined.
 */
@Service
public class DailyRecordService {

    private final UserRepository userRepository;
    private final MissionDefinitionRepository missionDefinitionRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DailyStatScoreRepository dailyStatScoreRepository;
    private final DailyScoreRepository dailyScoreRepository;
    private final ScoringService scoringService;

    public DailyRecordService(
            UserRepository userRepository,
            MissionDefinitionRepository missionDefinitionRepository,
            DailyRecordRepository dailyRecordRepository,
            DailyStatScoreRepository dailyStatScoreRepository,
            DailyScoreRepository dailyScoreRepository,
            ScoringService scoringService
    ) {
        this.userRepository = userRepository;
        this.missionDefinitionRepository = missionDefinitionRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.dailyStatScoreRepository = dailyStatScoreRepository;
        this.dailyScoreRepository = dailyScoreRepository;
        this.scoringService = scoringService;
    }

    @Transactional
    public MissionProgress saveRecord(Long userId, Long missionId, LocalDate date, Double inputValue, String photoUrl, String memo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        MissionDefinition mission = missionDefinitionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        DailyRecord record = dailyRecordRepository.findByUserIdAndMissionIdAndDate(userId, missionId, date)
                .orElseGet(DailyRecord::new);
        record.setUser(user);
        record.setMission(mission);
        record.setDate(date);
        record.setInputValue(inputValue);
        record.setPhotoUrl(photoUrl);
        record.setMemo(memo);
        dailyRecordRepository.save(record);

        List<MissionDefinition> missions = missionDefinitionRepository.findAllByOrderByIdAsc();
        Map<Long, DailyRecord> recordsByMission = recordsByMission(userId, date);
        Set<StatType> focusStats = focusStats(user);

        ScoringResult result = scoringService.calculate(toEntries(missions, recordsByMission), focusStats);

        dailyStatScoreRepository.deleteByUserIdAndDate(userId, date);
        result.statScores().forEach((stat, score) ->
                dailyStatScoreRepository.save(new DailyStatScore(null, user, date, stat, score)));

        DailyScore dailyScore = dailyScoreRepository.findByUserIdAndDate(userId, date).orElseGet(DailyScore::new);
        dailyScore.setUser(user);
        dailyScore.setDate(date);
        dailyScore.setMissionScore(result.missionScore());
        dailyScore.setBehaviorScore(result.behaviorScore());
        dailyScore.setStatScore(result.statScoreTotal());
        dailyScore.setBonusScore(result.bonusScore());
        dailyScore.setTotalScore(result.totalScore());
        dailyScore.setProgressPercent(result.progress());
        dailyScoreRepository.save(dailyScore);

        return new MissionProgress(
                result.totalScore(),
                result.progress(),
                result.statScores(),
                completedMissionNames(missions, recordsByMission),
                remainingMissionNames(missions, recordsByMission)
        );
    }

    @Transactional(readOnly = true)
    public MissionProgress getMissionProgress(Long userId, LocalDate date) {
        List<MissionDefinition> missions = missionDefinitionRepository.findAllByOrderByIdAsc();
        Map<Long, DailyRecord> recordsByMission = recordsByMission(userId, date);

        DailyScore dailyScore = dailyScoreRepository.findByUserIdAndDate(userId, date).orElse(null);
        Map<StatType, Double> statScores = dailyStatScoreRepository.findByUserIdAndDate(userId, date).stream()
                .collect(Collectors.toMap(DailyStatScore::getStatType, DailyStatScore::getScore, Double::sum, () -> new EnumMap<>(StatType.class)));

        double totalScore = dailyScore != null ? dailyScore.getTotalScore() : 0;
        double progress = dailyScore != null ? dailyScore.getProgressPercent() : 0;

        return new MissionProgress(
                totalScore,
                progress,
                statScores,
                completedMissionNames(missions, recordsByMission),
                remainingMissionNames(missions, recordsByMission)
        );
    }

    private Map<Long, DailyRecord> recordsByMission(Long userId, LocalDate date) {
        return dailyRecordRepository.findByUserIdAndDate(userId, date).stream()
                .collect(Collectors.toMap(r -> r.getMission().getId(), r -> r));
    }

    private Set<StatType> focusStats(User user) {
        return user.getStatFocus().stream()
                .map(UserStatFocus::getStatType)
                .collect(Collectors.toSet());
    }

    private List<MissionEntry> toEntries(List<MissionDefinition> missions, Map<Long, DailyRecord> recordsByMission) {
        return missions.stream()
                .map(m -> {
                    DailyRecord r = recordsByMission.get(m.getId());
                    return new MissionEntry(m, r != null ? r.getInputValue() : null);
                })
                .toList();
    }

    private List<String> completedMissionNames(List<MissionDefinition> missions, Map<Long, DailyRecord> recordsByMission) {
        List<String> completed = new ArrayList<>();
        for (MissionDefinition m : missions) {
            DailyRecord r = recordsByMission.get(m.getId());
            if (isComplete(m, r)) {
                completed.add(m.getName());
            }
        }
        return Collections.unmodifiableList(completed);
    }

    private List<String> remainingMissionNames(List<MissionDefinition> missions, Map<Long, DailyRecord> recordsByMission) {
        List<String> remaining = new ArrayList<>();
        for (MissionDefinition m : missions) {
            DailyRecord r = recordsByMission.get(m.getId());
            if (!isComplete(m, r)) {
                remaining.add(m.getName());
            }
        }
        return Collections.unmodifiableList(remaining);
    }

    private boolean isComplete(MissionDefinition mission, DailyRecord record) {
        return record != null
                && record.getInputValue() != null
                && mission.getTargetValue() != null
                && record.getInputValue() >= mission.getTargetValue();
    }
}
