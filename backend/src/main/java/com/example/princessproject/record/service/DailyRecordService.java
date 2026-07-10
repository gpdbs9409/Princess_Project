package com.example.princessproject.record.service;

import com.example.princessproject.project.model.UserGoal;
import com.example.princessproject.project.model.UserMission;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.model.UserStat;
import com.example.princessproject.project.repository.UserMissionRepository;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.record.model.DailyRecord;
import com.example.princessproject.record.model.VerificationStatus;
import com.example.princessproject.record.repository.DailyRecordRepository;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyRecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    private final UserProjectService userProjectService;
    private final ScoringService scoringService;

    public DailyRecordService(
            DailyRecordRepository dailyRecordRepository,
            UserMissionRepository userMissionRepository,
            UserRepository userRepository,
            UserProjectService userProjectService,
            ScoringService scoringService
    ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.userMissionRepository = userMissionRepository;
        this.userRepository = userRepository;
        this.userProjectService = userProjectService;
        this.scoringService = scoringService;
    }

    @Transactional
    public MissionProgress saveRecord(
            Long userId, Long userMissionId, LocalDate date, BigDecimal inputValue, String photoUrl, String memo
    ) {
        UserMission userMission = userMissionRepository.findById(userMissionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + userMissionId));
        UserProject project = userMission.getUserStat().getUserGoal().getProject();
        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Mission does not belong to user: " + userId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        BigDecimal targetValue = userMission.getTargetValue();
        BigDecimal assignedPoints = userMission.getAssignedPoints();
        BigDecimal achievementRate = scoringService.achievementRate(inputValue, targetValue);
        BigDecimal earnedScore = scoringService.earnedScore(assignedPoints, achievementRate);

        boolean requiresPhoto = userMission.getMissionDefinition() != null
                && userMission.getMissionDefinition().isRequiresPhoto();
        VerificationStatus verificationStatus;
        if (!requiresPhoto) {
            verificationStatus = VerificationStatus.NOT_REQUIRED;
        } else {
            verificationStatus = (photoUrl != null && !photoUrl.isBlank())
                    ? VerificationStatus.APPROVED
                    : VerificationStatus.PENDING;
        }

        DailyRecord record = dailyRecordRepository
                .findByUserIdAndUserMissionIdAndRecordDate(userId, userMissionId, date)
                .orElseGet(DailyRecord::new);
        record.setUser(user);
        record.setProject(project);
        record.setUserMission(userMission);
        record.setRecordDate(date);
        record.setInputValue(inputValue);
        record.setPhotoUrl(photoUrl);
        record.setMemo(memo);
        record.setTargetValueSnapshot(targetValue);
        record.setAssignedPointsSnapshot(assignedPoints);
        record.setAchievementRate(achievementRate);
        record.setEarnedScore(earnedScore);
        record.setVerificationStatus(verificationStatus);
        dailyRecordRepository.save(record);

        return getMissionProgress(userId, date);
    }

    @Transactional(readOnly = true)
    public MissionProgress getMissionProgress(Long userId, LocalDate date) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenActiveMissions(project);

        List<DailyRecord> records = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, date, date);
        Map<Long, DailyRecord> recordsByMissionId = new LinkedHashMap<>();
        for (DailyRecord record : records) {
            recordsByMissionId.put(record.getUserMission().getId(), record);
        }

        Map<String, BigDecimal> statScores = new LinkedHashMap<>();
        List<String> completed = new ArrayList<>();
        List<String> remaining = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ActiveMission active : activeMissions) {
            UserMission mission = active.mission();
            DailyRecord record = recordsByMissionId.get(mission.getId());
            String statKey = active.goalTypeCode().toLowerCase();

            if (record != null) {
                totalScore = totalScore.add(record.getEarnedScore());
                statScores.merge(statKey, record.getEarnedScore(), BigDecimal::add);

                boolean isComplete = record.getInputValue().compareTo(record.getTargetValueSnapshot()) >= 0;
                (isComplete ? completed : remaining).add(mission.displayName());
            } else {
                statScores.putIfAbsent(statKey, BigDecimal.ZERO);
                remaining.add(mission.displayName());
            }
        }

        BigDecimal maxPossible = activeMissions.stream()
                .map(active -> active.mission().getAssignedPoints())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal progress = maxPossible.signum() > 0
                ? totalScore.divide(maxPossible, 4, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MissionProgress(totalScore, progress, statScores, completed, remaining);
    }

    private record ActiveMission(UserMission mission, String goalTypeCode) {
    }

    private List<ActiveMission> flattenActiveMissions(UserProject project) {
        List<ActiveMission> missions = new ArrayList<>();
        for (UserGoal goal : project.getGoals()) {
            String goalTypeCode = goal.getGoalType().getCode().name();
            for (UserStat stat : goal.getStats()) {
                if (!stat.isActive()) {
                    continue;
                }
                for (UserMission mission : stat.getMissions()) {
                    if (mission.isActive()) {
                        missions.add(new ActiveMission(mission, goalTypeCode));
                    }
                }
            }
        }
        return missions;
    }
}
