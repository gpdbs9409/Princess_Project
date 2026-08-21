package com.example.princessproject.record.service;

import com.example.princessproject.catalog.model.MissionType;
import com.example.princessproject.record.dto.TodayRecordEntry;
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
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyRecordService {

    // A record's input is compared against its mission's own target so this scales naturally
    // (a "10000걸음" mission and a "30분" mission both get the same generous headroom above
    // their own target) instead of one fixed number that would be way too tight for some
    // missions and useless for others. This exists purely to catch fat-finger/garbage input
    // (e.g. "999999" typed into a 30-minute mission) - genuine overachievement stays well
    // inside it, and the score itself is already capped at 100% by ScoringService regardless.
    private static final BigDecimal MAX_INPUT_MULTIPLE_OF_TARGET = BigDecimal.valueOf(50);

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
            Long userId, Long userMissionId, LocalDate date, BigDecimal inputValue, String photoUrl, String memo,
            Boolean aiVerified
    ) {
        UserMission userMission = userMissionRepository.findById(userMissionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + userMissionId));
        UserProject project = userMission.getUserStat().getUserGoal().getProject();
        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Mission does not belong to user: " + userId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (photoUrl == null || photoUrl.isBlank()) {
            throw new RecordValidationException("PHOTO_REQUIRED", "A photo is required to save a record");
        }

        BigDecimal targetValue = userMission.getTargetValue();
        BigDecimal assignedPoints = userMission.getAssignedPoints();

        // Belt-and-suspenders alongside RecordRequest's @DecimalMin(0) - this is the layer that
        // also catches obviously-bogus overachievement (e.g. "999999" against a 30분 target),
        // which no static DTO annotation can express since it depends on this mission's own
        // target value.
        if (inputValue.signum() < 0) {
            throw new RecordValidationException("INPUT_NEGATIVE", "Input value cannot be negative");
        }
        if (targetValue != null && targetValue.signum() > 0) {
            BigDecimal maxReasonableInput = targetValue.multiply(MAX_INPUT_MULTIPLE_OF_TARGET);
            if (inputValue.compareTo(maxReasonableInput) > 0) {
                throw new RecordValidationException(
                        "INPUT_TOO_LARGE",
                        "Input value " + inputValue + " is unreasonably larger than the mission's target " + targetValue);
            }
        }

        // Each record still snapshots its own day's achievement, independent of how WEEKLY
        // missions get rolled up for display (see getMissionProgress) - this is just this
        // record's own contribution, kept for history/auditing.
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
        record.setAiVerified(aiVerified);
        dailyRecordRepository.save(record);

        return getMissionProgress(userId, date);
    }

    /**
     * Progress "as of" a given day. DAILY missions compare that day's own record to their
     * target. WEEKLY missions compare the week-to-date sum (Monday..date) of their inputs to
     * their target, so the weekly goal fills in gradually across the week rather than expecting
     * the full weekly amount in a single day's record.
     */
    // Not readOnly: getOrCreateActive() inserts a new project on a user's very first call, and a
    // readOnly transaction puts the JDBC connection itself in read-only mode, which fails that
    // insert for a brand-new user who hasn't had a project created yet.
    @Transactional
    public MissionProgress getMissionProgress(Long userId, LocalDate date) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenActiveMissions(project);

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DailyRecord> records = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, date);

        return computeProgress(activeMissions, records, weekStart, date);
    }

    /**
     * Same per-day math as {@link #getMissionProgress}, but for all 7 days of a week at once,
     * in a single DB round trip instead of 7 (one per day). Callers that need a whole week's
     * worth of daily snapshots - the weekly report, and the admin weekly-refund tracker which
     * does this once per participant - used to call getMissionProgress() in a day-by-day loop,
     * which re-queried an overlapping, ever-growing date range on every iteration (Monday..day1,
     * Monday..day2, ...). That's fine for a single user's own dashboard but multiplies badly
     * once it runs once per member in a cohort.
     */
    @Transactional
    public List<MissionProgress> getWeekDailyProgress(Long userId, LocalDate weekStart) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenActiveMissions(project);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyRecord> weekRecords = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);

        List<MissionProgress> result = new ArrayList<>(7);
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            // weekRecords already covers the whole week - computeProgress only needs the
            // slice up to `date` for "week-to-date", so hand it the same in-memory list each
            // time (it filters by date itself) instead of re-hitting the DB.
            result.add(computeProgress(activeMissions, weekRecords, weekStart, date));
        }
        return result;
    }

    private MissionProgress computeProgress(
            List<ActiveMission> activeMissions, List<DailyRecord> recordsInRange, LocalDate weekStart, LocalDate date
    ) {
        Map<Long, DailyRecord> todaysRecordByMissionId = new LinkedHashMap<>();
        Map<Long, BigDecimal> weekToDateInputByMissionId = new LinkedHashMap<>();
        for (DailyRecord record : recordsInRange) {
            LocalDate recordDate = record.getRecordDate();
            if (recordDate.isBefore(weekStart) || recordDate.isAfter(date)) {
                continue;
            }
            Long missionId = record.getUserMission().getId();
            if (recordDate.equals(date)) {
                todaysRecordByMissionId.put(missionId, record);
            }
            weekToDateInputByMissionId.merge(missionId, record.getInputValue(), BigDecimal::add);
        }

        Map<String, BigDecimal> statScores = new LinkedHashMap<>();
        List<String> completed = new ArrayList<>();
        List<String> remaining = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ActiveMission active : activeMissions) {
            UserMission mission = active.mission();
            String statKey = active.goalTypeCode().toLowerCase();
            statScores.putIfAbsent(statKey, BigDecimal.ZERO);

            boolean isComplete;
            BigDecimal earnedScore;

            if (active.missionType() == MissionType.WEEKLY) {
                BigDecimal weekToDateInput = weekToDateInputByMissionId.getOrDefault(mission.getId(), BigDecimal.ZERO);
                BigDecimal rate = scoringService.achievementRate(weekToDateInput, mission.getTargetValue());
                earnedScore = scoringService.earnedScore(mission.getAssignedPoints(), rate);
                isComplete = weekToDateInput.compareTo(mission.getTargetValue()) >= 0;
            } else {
                DailyRecord todaysRecord = todaysRecordByMissionId.get(mission.getId());
                if (todaysRecord == null) {
                    remaining.add(mission.displayName());
                    continue;
                }
                earnedScore = todaysRecord.getEarnedScore();
                isComplete = todaysRecord.getInputValue().compareTo(todaysRecord.getTargetValueSnapshot()) >= 0;
            }

            totalScore = totalScore.add(earnedScore);
            statScores.merge(statKey, earnedScore, BigDecimal::add);
            (isComplete ? completed : remaining).add(mission.displayName());
        }

        BigDecimal maxPossible = activeMissions.stream()
                .map(active -> active.mission().getAssignedPoints())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal progress = maxPossible.signum() > 0
                ? totalScore.divide(maxPossible, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<Long, TodayRecordEntry> todayRecords = new LinkedHashMap<>();
        for (ActiveMission active : activeMissions) {
            DailyRecord todaysRecord = todaysRecordByMissionId.get(active.mission().getId());
            if (todaysRecord != null) {
                todayRecords.put(active.mission().getId(), TodayRecordEntry.from(todaysRecord));
            }
        }

        return new MissionProgress(totalScore, progress, statScores, completed, remaining, todayRecords);
    }

    /**
     * The whole week's total, counting each WEEKLY mission's contribution exactly once (its
     * full-week sum vs target) instead of once per day - summing {@link #getMissionProgress}
     * across all 7 days would multiply-count WEEKLY missions since their week-to-date score
     * only grows across the week.
     */
    // Not readOnly: same reason as getMissionProgress above.
    @Transactional
    public MissionProgress getWeekTotalProgress(Long userId, LocalDate weekStart) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenActiveMissions(project);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyRecord> records = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);
        Map<Long, List<DailyRecord>> recordsByMissionId = new LinkedHashMap<>();
        for (DailyRecord record : records) {
            recordsByMissionId.computeIfAbsent(record.getUserMission().getId(), k -> new ArrayList<>()).add(record);
        }

        Map<String, BigDecimal> statScores = new LinkedHashMap<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxPossible = BigDecimal.ZERO;

        for (ActiveMission active : activeMissions) {
            UserMission mission = active.mission();
            String statKey = active.goalTypeCode().toLowerCase();
            statScores.putIfAbsent(statKey, BigDecimal.ZERO);
            List<DailyRecord> missionRecords = recordsByMissionId.getOrDefault(mission.getId(), List.of());

            BigDecimal earnedScore;
            if (active.missionType() == MissionType.WEEKLY) {
                BigDecimal weekSum = missionRecords.stream().map(DailyRecord::getInputValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal rate = scoringService.achievementRate(weekSum, mission.getTargetValue());
                earnedScore = scoringService.earnedScore(mission.getAssignedPoints(), rate);
                maxPossible = maxPossible.add(mission.getAssignedPoints());
            } else {
                earnedScore = missionRecords.stream().map(DailyRecord::getEarnedScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                maxPossible = maxPossible.add(mission.getAssignedPoints().multiply(BigDecimal.valueOf(7)));
            }

            totalScore = totalScore.add(earnedScore);
            statScores.merge(statKey, earnedScore, BigDecimal::add);
        }

        BigDecimal progress = maxPossible.signum() > 0
                ? totalScore.divide(maxPossible, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MissionProgress(totalScore, progress, statScores, List.of(), List.of(), Map.of());
    }

    private record ActiveMission(UserMission mission, String goalTypeCode, MissionType missionType) {
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
                        missions.add(new ActiveMission(mission, goalTypeCode, mission.getMissionType()));
                    }
                }
            }
        }
        return missions;
    }
}
