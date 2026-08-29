package com.example.princessproject.record.service;

import com.example.princessproject.catalog.model.MissionType;
import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import com.example.princessproject.commontask.repository.CommonTaskRecordRepository;
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
    private static final BigDecimal COMMON_TASK_POINTS = BigDecimal.valueOf(20);

    private final DailyRecordRepository dailyRecordRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    private final UserProjectService userProjectService;
    private final ScoringService scoringService;
    private final CommonTaskRecordRepository commonTaskRecordRepository;

    public DailyRecordService(
            DailyRecordRepository dailyRecordRepository,
            UserMissionRepository userMissionRepository,
            UserRepository userRepository,
            UserProjectService userProjectService,
            ScoringService scoringService,
            CommonTaskRecordRepository commonTaskRecordRepository
    ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.userMissionRepository = userMissionRepository;
        this.userRepository = userRepository;
        this.userProjectService = userProjectService;
        this.scoringService = scoringService;
        this.commonTaskRecordRepository = commonTaskRecordRepository;
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
        List<CommonTaskRecord> commonRecords = commonTaskRecordRepository
                .findByUserIdAndRecordDateBetweenAndTaskTypeIn(userId, weekStart, date, List.of(CommonTaskType.values()));

        return computeProgress(activeMissions, records, commonRecords, weekStart, date);
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
        return getWeekDailyProgress(userId, weekStart, false);
    }

    /** Daily-only snapshots for refund attendance. WEEKLY goals must not make later or future
     * days look completed merely because their cumulative target was reached earlier. */
    @Transactional
    public List<MissionProgress> getWeekDailyRefundProgress(Long userId, LocalDate weekStart) {
        return getWeekDailyProgress(userId, weekStart, true);
    }

    /**
     * WEEKLY 미션이 그 주 목표를 채웠는지를 주 단위로 딱 한 번 판정한다.
     *
     * 환급 심사에서 WEEKLY를 일별 성공일수에 섞으면 두 방향으로 다 틀어진다. 목표를 채우기
     * 전(주 초반)에는 아직 못 채웠다는 이유로 매일 감점되고, 채운 뒤(주 후반)에는 아무것도
     * 하지 않아도 누적값 덕분에 매일 완료로 잡힌다. 그래서 일별 판정에서는 빼고(dailyOnly),
     * 대신 "주간 미션을 전부 달성했는가"를 환급의 별도 조건으로 쓴다.
     */
    @Transactional
    public WeeklyMissionStatus getWeeklyMissionStatus(Long userId, LocalDate weekStart) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> weeklyMissions = flattenActiveMissions(project).stream()
                .filter(active -> active.missionType() == MissionType.WEEKLY)
                .toList();
        if (weeklyMissions.isEmpty()) {
            return new WeeklyMissionStatus(0, 0);
        }

        LocalDate weekEnd = weekStart.plusDays(6);
        Map<Long, BigDecimal> weekSumByMissionId = new LinkedHashMap<>();
        for (DailyRecord record : dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd)) {
            weekSumByMissionId.merge(record.getUserMission().getId(), record.getInputValue(), BigDecimal::add);
        }

        int achieved = 0;
        for (ActiveMission active : weeklyMissions) {
            BigDecimal target = active.mission().getTargetValue();
            BigDecimal weekSum = weekSumByMissionId.getOrDefault(active.mission().getId(), BigDecimal.ZERO);
            if (target != null && target.signum() > 0 && weekSum.compareTo(target) >= 0) {
                achieved++;
            }
        }
        return new WeeklyMissionStatus(weeklyMissions.size(), achieved);
    }

    /** 그 주의 WEEKLY 미션 개수와 그중 목표를 채운 개수. total이 0이면 조건 자체가 없는 것으로 본다. */
    public record WeeklyMissionStatus(int total, int achieved) {
        public boolean allAchieved() {
            return total == 0 || achieved >= total;
        }
    }

    private List<MissionProgress> getWeekDailyProgress(Long userId, LocalDate weekStart, boolean dailyOnly) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenActiveMissions(project);
        if (dailyOnly) {
            activeMissions = activeMissions.stream()
                    .filter(active -> active.missionType() == MissionType.DAILY)
                    .toList();
        }
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyRecord> weekRecords = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);
        List<CommonTaskRecord> commonRecords = commonTaskRecordRepository
                .findByUserIdAndRecordDateBetweenAndTaskTypeIn(userId, weekStart, weekEnd, List.of(CommonTaskType.values()));

        List<MissionProgress> result = new ArrayList<>(7);
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            // weekRecords already covers the whole week - computeProgress only needs the
            // slice up to `date` for "week-to-date", so hand it the same in-memory list each
            // time (it filters by date itself) instead of re-hitting the DB.
            result.add(computeProgress(activeMissions, weekRecords, commonRecords, weekStart, date));
        }
        return result;
    }

    private MissionProgress computeProgress(
            List<ActiveMission> activeMissions, List<DailyRecord> recordsInRange,
            List<CommonTaskRecord> commonRecords, LocalDate weekStart, LocalDate date
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
        List<MissionProgressDetail> missionDetails = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ActiveMission active : activeMissions) {
            UserMission mission = active.mission();
            String statKey = active.goalTypeCode().toLowerCase();
            statScores.putIfAbsent(statKey, BigDecimal.ZERO);

            boolean isComplete;
            BigDecimal earnedScore;
            BigDecimal actualValue;
            BigDecimal achievementRate;

            if (active.missionType() == MissionType.WEEKLY) {
                BigDecimal weekToDateInput = weekToDateInputByMissionId.getOrDefault(mission.getId(), BigDecimal.ZERO);
                actualValue = weekToDateInput;
                achievementRate = scoringService.achievementRate(actualValue, mission.getTargetValue());
                earnedScore = scoringService.earnedScore(mission.getAssignedPoints(), achievementRate);
                isComplete = weekToDateInput.compareTo(mission.getTargetValue()) >= 0;
            } else {
                DailyRecord todaysRecord = todaysRecordByMissionId.get(mission.getId());
                if (todaysRecord == null) {
                    missionDetails.add(new MissionProgressDetail(
                            mission.displayName(), active.goalTypeCode(), active.missionType(), mission.getTargetValue(),
                            BigDecimal.ZERO, mission.getAssignedPoints(), BigDecimal.ZERO, BigDecimal.ZERO, false));
                    remaining.add(mission.displayName());
                    continue;
                }
                actualValue = todaysRecord.getInputValue();
                earnedScore = todaysRecord.getEarnedScore();
                achievementRate = todaysRecord.getAchievementRate();
                isComplete = todaysRecord.getInputValue().compareTo(todaysRecord.getTargetValueSnapshot()) >= 0;
            }

            totalScore = totalScore.add(earnedScore);
            statScores.merge(statKey, earnedScore, BigDecimal::add);
            (isComplete ? completed : remaining).add(mission.displayName());
            missionDetails.add(new MissionProgressDetail(
                    mission.displayName(), active.goalTypeCode(), active.missionType(), mission.getTargetValue(),
                    actualValue, mission.getAssignedPoints(), earnedScore, achievementRate, isComplete));
        }

        for (CommonMissionScore common : commonMissionScores(commonRecords, date)) {
            totalScore = totalScore.add(common.earnedScore());
            (common.completed() ? completed : remaining).add(common.name());
            missionDetails.add(common.toDetail());
        }

        BigDecimal maxPossible = activeMissions.stream()
                .map(active -> active.mission().getAssignedPoints())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 점수가 붙는 공통과제는 독서·공부 둘뿐이다 (주간 회고는 점수 대상 아님).
        maxPossible = maxPossible.add(COMMON_TASK_POINTS.multiply(BigDecimal.valueOf(2)));
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

        return new MissionProgress(totalScore, progress, statScores, completed, remaining, todayRecords, missionDetails);
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
        List<CommonTaskRecord> commonRecords = commonTaskRecordRepository
                .findByUserIdAndRecordDateBetweenAndTaskTypeIn(userId, weekStart, weekEnd, List.of(CommonTaskType.values()));
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


        // 주간 회고는 점수 대상이 아니다 (2026-08 결정): 작성 여부와 무관하게 총점·만점 어디에도
        // 반영하지 않는다. 점수가 붙는 공통과제는 독서·공부 둘뿐이다.
        for (CommonTaskRecord record : commonRecords) {
            if (record.getTaskType() == CommonTaskType.WEEKLY_RETROSPECTIVE) continue;
            CommonMissionScore score = scoreDailyCommonTask(record);
            totalScore = totalScore.add(score.earnedScore());
        }
        // 독서·공부 2종 × 7일
        maxPossible = maxPossible.add(COMMON_TASK_POINTS.multiply(BigDecimal.valueOf(14)));

        BigDecimal progress = maxPossible.signum() > 0
                ? totalScore.divide(maxPossible, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MissionProgress(totalScore, progress, statScores, List.of(), List.of(), Map.of(), List.of());
    }

    private record ActiveMission(UserMission mission, String goalTypeCode, MissionType missionType) {
    }

    private List<CommonMissionScore> commonMissionScores(List<CommonTaskRecord> records, LocalDate date) {
        List<CommonMissionScore> scores = new ArrayList<>();
        records.stream()
                .filter(record -> record.getRecordDate().equals(date))
                .filter(record -> record.getTaskType() != CommonTaskType.WEEKLY_RETROSPECTIVE)
                .map(this::scoreDailyCommonTask)
                .forEach(scores::add);

        for (CommonTaskType type : List.of(CommonTaskType.READING, CommonTaskType.STUDY)) {
            if (scores.stream().noneMatch(score -> score.taskType() == type)) {
                String name = type == CommonTaskType.READING ? "독서" : "공부";
                scores.add(new CommonMissionScore(type, name, "common", MissionType.DAILY,
                        commonTarget(type, null), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
            }
        }

        return scores;
    }

    /**
     * 독서는 수행 여부(T/F)로만 채점한다.
     *
     * 기획의 독서 지표는 "주간 달성률 = 독서일 / 7"인데, 이건 주 단위 값이라 하루 점수에
     * 섞을 수가 없다. 넣는 순간 월요일에 아무리 열심히 해도 최대 1/7(약 14%)이 되어
     * 하루 100점이 구조적으로 불가능해지기 때문이다. 그래서 하루 단위에서는 "오늘 읽었는가"만
     * 보고, 주간 달성률은 별도 지표로 다룬다.
     *
     * 공부는 그날의 계획 대비 완료(완료/계획)라 그 자체가 일 단위 비율이므로 비례 채점을
     * 유지한다.
     */
    private CommonMissionScore scoreDailyCommonTask(CommonTaskRecord record) {
        if (record.getTaskType() == CommonTaskType.READING) {
            int pages = Math.max(0, (record.getEndPage() == null ? 0 : record.getEndPage())
                    - (record.getStartPage() == null ? 0 : record.getStartPage()));
            // 기록을 남겼으면 완료. 페이지 수는 참고용으로만 실적에 담는다.
            boolean done = record.getStartPage() != null || record.getEndPage() != null;
            BigDecimal rate = done ? BigDecimal.ONE : BigDecimal.ZERO;
            return new CommonMissionScore(CommonTaskType.READING, "독서", "common", MissionType.DAILY,
                    BigDecimal.ONE, BigDecimal.valueOf(pages),
                    scoringService.earnedScore(COMMON_TASK_POINTS, rate), rate, done);
        }

        BigDecimal actual = record.getStudyCompletedAmount() == null
                ? BigDecimal.ZERO : record.getStudyCompletedAmount();
        BigDecimal target = commonTarget(record.getTaskType(), record);
        BigDecimal rate = scoringService.achievementRate(actual, target);
        return new CommonMissionScore(record.getTaskType(), "공부", "common", MissionType.DAILY,
                target, actual, scoringService.earnedScore(COMMON_TASK_POINTS, rate), rate,
                actual.compareTo(target) >= 0);
    }

    private BigDecimal commonTarget(CommonTaskType type, CommonTaskRecord record) {
        // 독서는 T/F라 목표가 1(= 했다)이다. 페이지 수는 채점에 쓰지 않는다.
        if (type == CommonTaskType.READING) return BigDecimal.ONE;
        if (record != null && record.getStudyPlannedAmount() != null && record.getStudyPlannedAmount().signum() > 0) {
            return record.getStudyPlannedAmount();
        }
        return BigDecimal.ONE;
    }

    private record CommonMissionScore(
            CommonTaskType taskType, String name, String goalTypeCode, MissionType missionType,
            BigDecimal target, BigDecimal actual, BigDecimal earnedScore, BigDecimal achievementRate,
            boolean completed
    ) {
        MissionProgressDetail toDetail() {
            return new MissionProgressDetail(name, goalTypeCode, missionType, target, actual,
                    COMMON_TASK_POINTS, earnedScore, achievementRate, completed);
        }
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
