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
    // 하루 만점을 100점으로 고정한다. 예전에는 미션마다 시딩된 배점(운동 20점, 식단 10점...)을
    // 그대로 더해서 만점이 참가자마다 71점, 111점, 240점으로 제각각이었고, 정작 참가자가 고른
    // 자본 비중(%)은 점수에 전혀 쓰이지 않았다. 그래서 "신체 70%"를 준 사람이 그 자본에 미션을
    // 하나만 걸면 실제로는 경제가 3배 중요해지는, 설정과 정반대인 결과가 나왔다.
    private static final BigDecimal DAILY_MAX_POINTS = BigDecimal.valueOf(100);
    private static final BigDecimal COMMON_TASK_TOTAL_POINTS = BigDecimal.valueOf(20);
    private static final BigDecimal MISSION_TOTAL_POINTS = BigDecimal.valueOf(80);
    /** 독서·공부 각각의 배점 (20점을 둘로 나눔). */
    private static final BigDecimal COMMON_TASK_POINTS = BigDecimal.valueOf(10);

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

        // 이 값은 그 기록 자체의 스냅샷(감사용)이다. 화면에 보이는 점수는 조회 시점에
        // 자본 비중으로 다시 계산하므로 여기 저장된 earnedScore를 쓰지 않는다.
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
     * Progress "as of" a given day - 그날의 DAILY 미션 기록과 목표를 비교한다.
     *
     * WEEKLY 미션은 스펙아웃되어 채점 대상이 아니다 (flattenScoredMissions 참고).
     */
    // Not readOnly: getOrCreateActive() inserts a new project on a user's very first call, and a
    // readOnly transaction puts the JDBC connection itself in read-only mode, which fails that
    // insert for a brand-new user who hasn't had a project created yet.
    @Transactional
    public MissionProgress getMissionProgress(Long userId, LocalDate date) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenScoredMissions(project);

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DailyRecord> records = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, date);
        List<CommonTaskRecord> commonRecords = latestCommonRecords(commonTaskRecordRepository
                .findByUserIdAndRecordDateBetweenAndTaskTypeIn(
                        userId, weekStart, date, List.of(CommonTaskType.values())));

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
        return getWeekDailyProgressInternal(userId, weekStart);
    }

    /** 환급 출석용 일별 스냅샷. DAILY 개인 미션과 그날의 독서·공부만 포함한다. */
    @Transactional
    public List<MissionProgress> getWeekDailyRefundProgress(Long userId, LocalDate weekStart) {
        return getWeekDailyProgressInternal(userId, weekStart);
    }

    private List<MissionProgress> getWeekDailyProgressInternal(Long userId, LocalDate weekStart) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenScoredMissions(project);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyRecord> weekRecords = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);
        List<CommonTaskRecord> commonRecords = latestCommonRecords(commonTaskRecordRepository
                .findByUserIdAndRecordDateBetweenAndTaskTypeIn(
                        userId, weekStart, weekEnd, List.of(CommonTaskType.values())));

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
            if (record.isAdminInvalidated()) continue;
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

        Map<Long, BigDecimal> pointsByMissionId = missionPoints(activeMissions);
        Map<String, BigDecimal> statScores = new LinkedHashMap<>();
        List<String> completed = new ArrayList<>();
        List<String> remaining = new ArrayList<>();
        List<MissionProgressDetail> missionDetails = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ActiveMission active : activeMissions) {
            UserMission mission = active.mission();
            BigDecimal points = pointsByMissionId.getOrDefault(mission.getId(), BigDecimal.ZERO);
            String statKey = active.goalTypeCode().toLowerCase();
            statScores.putIfAbsent(statKey, BigDecimal.ZERO);

            boolean isComplete;
            BigDecimal earnedScore;
            BigDecimal actualValue;
            BigDecimal achievementRate;

            {
                DailyRecord todaysRecord = todaysRecordByMissionId.get(mission.getId());
                if (todaysRecord == null) {
                    missionDetails.add(new MissionProgressDetail(
                            mission.displayName(), active.goalTypeCode(), active.missionType(), mission.getTargetValue(),
                            BigDecimal.ZERO, points, BigDecimal.ZERO, BigDecimal.ZERO, false));
                    remaining.add(mission.displayName());
                    continue;
                }
                actualValue = todaysRecord.getInputValue();
                achievementRate = todaysRecord.getAchievementRate();
                // 저장 시점에 계산해둔 earnedScore는 옛 배점 기준이라 쓰지 않는다. 달성률만
                // 가져와서 지금 비중으로 다시 곱한다 (비중이 바뀌어도 과거 기록이 따라온다).
                earnedScore = scoringService.earnedScore(points, achievementRate);
                isComplete = todaysRecord.getInputValue().compareTo(todaysRecord.getTargetValueSnapshot()) >= 0;
            }

            totalScore = totalScore.add(earnedScore);
            statScores.merge(statKey, earnedScore, BigDecimal::add);
            (isComplete ? completed : remaining).add(mission.displayName());
            missionDetails.add(new MissionProgressDetail(
                    mission.displayName(), active.goalTypeCode(), active.missionType(), mission.getTargetValue(),
                    actualValue, points, earnedScore, achievementRate, isComplete));
        }

        for (CommonMissionScore common : commonMissionScores(commonRecords, date)) {
            totalScore = totalScore.add(common.earnedScore());
            (common.completed() ? completed : remaining).add(common.name());
            missionDetails.add(common.toDetail());
        }

        // 만점은 항상 100점 (개인 미션 80 + 공통과제 20). 미션을 아직 설정하지 않았다면
        // 개인 미션 몫은 배분할 곳이 없으므로 공통과제 20점만 만점이 된다.
        BigDecimal maxPossible = activeMissions.isEmpty()
                ? COMMON_TASK_TOTAL_POINTS
                : DAILY_MAX_POINTS;
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

        return new MissionProgress(totalScore, progress, statScores, completed, remaining, todayRecords,
                missionDetails, maxPossible);
    }

    /** 일별 개인 미션과 독서·공부 점수를 7일간 합산한 주간 총점. 회고는 제외한다. */
    // Not readOnly: same reason as getMissionProgress above.
    @Transactional
    public MissionProgress getWeekTotalProgress(Long userId, LocalDate weekStart) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        List<ActiveMission> activeMissions = flattenScoredMissions(project);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyRecord> records = dailyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);
        List<CommonTaskRecord> commonRecords = latestCommonRecords(commonTaskRecordRepository
                .findByUserIdAndRecordDateBetweenAndTaskTypeIn(
                        userId, weekStart, weekEnd, List.of(CommonTaskType.values())));
        Map<Long, List<DailyRecord>> recordsByMissionId = new LinkedHashMap<>();
        for (DailyRecord record : records) {
            recordsByMissionId.computeIfAbsent(record.getUserMission().getId(), k -> new ArrayList<>()).add(record);
        }

        Map<Long, BigDecimal> pointsByMissionId = missionPoints(activeMissions);
        Map<String, BigDecimal> statScores = new LinkedHashMap<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ActiveMission active : activeMissions) {
            UserMission mission = active.mission();
            String statKey = active.goalTypeCode().toLowerCase();
            statScores.putIfAbsent(statKey, BigDecimal.ZERO);
            BigDecimal points = pointsByMissionId.getOrDefault(mission.getId(), BigDecimal.ZERO);
            List<DailyRecord> missionRecords = recordsByMissionId.getOrDefault(mission.getId(), List.of());

            // 하루치 달성률 × 그 미션의 배점을 7일간 더한다. 저장된 earnedScore는 옛 배점
            // 기준이라 쓰지 않고, 달성률만 꺼내 현재 비중으로 다시 계산한다.
            BigDecimal earnedScore = missionRecords.stream()
                    .filter(record -> !record.isAdminInvalidated())
                    .map(record -> scoringService.earnedScore(points, record.getAchievementRate()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalScore = totalScore.add(earnedScore);
            statScores.merge(statKey, earnedScore, BigDecimal::add);
        }

        // 주간 만점 = 하루 만점 × 7. 하루 만점이 100으로 고정이라 주간도 700으로 고정된다.
        BigDecimal maxPossible = (activeMissions.isEmpty() ? COMMON_TASK_TOTAL_POINTS : DAILY_MAX_POINTS)
                .multiply(BigDecimal.valueOf(7));


        // 이 저장소에는 점수가 붙는 일일 독서·공부만 존재한다. 주간 회고는 별도 저장소다.
        for (CommonTaskRecord record : commonRecords) {
            CommonMissionScore score = scoreDailyCommonTask(record);
            totalScore = totalScore.add(score.earnedScore());
        }

        BigDecimal progress = maxPossible.signum() > 0
                ? totalScore.divide(maxPossible, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MissionProgress(totalScore, progress, statScores, List.of(), List.of(), Map.of(),
                List.of(), maxPossible);
    }

    private record ActiveMission(
            UserMission mission, String goalTypeCode, MissionType missionType, int goalWeightPercent) {
    }

    /** unique 제약이 없던 배포에서 생긴 같은 날짜·타입 중복은 가장 최신 id 한 건만 채점한다. */
    private List<CommonTaskRecord> latestCommonRecords(List<CommonTaskRecord> records) {
        Map<String, CommonTaskRecord> latestByDateAndType = new LinkedHashMap<>();
        for (CommonTaskRecord record : records) {
            if (record.isAdminInvalidated()) continue;
            String key = record.getRecordDate() + ":" + record.getTaskType();
            latestByDateAndType.merge(key, record,
                    (left, right) -> left.getId() >= right.getId() ? left : right);
        }
        return new ArrayList<>(latestByDateAndType.values());
    }

    private List<CommonMissionScore> commonMissionScores(List<CommonTaskRecord> records, LocalDate date) {
        List<CommonMissionScore> scores = new ArrayList<>();
        records.stream()
                .filter(record -> record.getRecordDate().equals(date))
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

    /**
     * 점수 계산에 쓰이는 미션 = DAILY만.
     *
     * WEEKLY 미션은 2026-08-29부터 점수 체인(오늘/주간/최종)에서 완전히 제외한다. 누적
     * 방식이라 하루 점수에 넣으면 실제로 수행하지 않은 날에도 점수가 그대로 들어가고
     * (목표를 채운 뒤에는 남은 요일이 자동 만점), 7일치를 더한 값과 주간 계산값이 서로
     * 달라져서 "오늘 × 7 = 주간 × 4 = 최종"이라는 구조 자체가 성립하지 않기 때문이다.

     */
    /**
     * 미션별 배점을 자본 비중에서 산출한다.
     *
     *   개인 미션 80점  →  자본 비중(%)대로 배분  →  자본 안에서 미션끼리 균등 분배
     *
     * 미션이 하나도 없는 자본은 몫을 쓸 수 없으므로, 미션이 있는 자본들의 비중으로 다시
     * 정규화해서 80점이 남김없이 배분되게 한다. 그래야 만점이 항상 100점으로 유지된다.
     */
    private Map<Long, BigDecimal> missionPoints(List<ActiveMission> activeMissions) {
        Map<String, List<ActiveMission>> byGoal = new LinkedHashMap<>();
        Map<String, Integer> weightByGoal = new LinkedHashMap<>();
        for (ActiveMission active : activeMissions) {
            byGoal.computeIfAbsent(active.goalTypeCode(), k -> new ArrayList<>()).add(active);
            weightByGoal.putIfAbsent(active.goalTypeCode(), active.goalWeightPercent());
        }

        int weightSum = weightByGoal.values().stream().mapToInt(Integer::intValue).sum();
        Map<Long, BigDecimal> points = new LinkedHashMap<>();
        if (activeMissions.isEmpty()) {
            return points;
        }

        for (Map.Entry<String, List<ActiveMission>> entry : byGoal.entrySet()) {
            List<ActiveMission> goalMissions = entry.getValue();
            // 비중이 하나도 없으면(전부 0 또는 미설정) 자본끼리 균등하게 나눈다.
            BigDecimal goalShare = weightSum > 0
                    ? MISSION_TOTAL_POINTS
                            .multiply(BigDecimal.valueOf(weightByGoal.get(entry.getKey())))
                            .divide(BigDecimal.valueOf(weightSum), 4, RoundingMode.HALF_UP)
                    : MISSION_TOTAL_POINTS.divide(BigDecimal.valueOf(byGoal.size()), 4, RoundingMode.HALF_UP);
            BigDecimal perMission = goalShare.divide(BigDecimal.valueOf(goalMissions.size()), 4, RoundingMode.HALF_UP);
            for (ActiveMission active : goalMissions) {
                points.put(active.mission().getId(), perMission);
            }
        }
        return points;
    }

    private List<ActiveMission> flattenScoredMissions(UserProject project) {
        return flattenActiveMissions(project).stream()
                .filter(active -> active.missionType() == MissionType.DAILY)
                .toList();
    }

    private List<ActiveMission> flattenActiveMissions(UserProject project) {
        List<ActiveMission> missions = new ArrayList<>();
        for (UserGoal goal : project.getGoals()) {
            String goalTypeCode = goal.getGoalType().getCode().name();
            int goalWeight = goal.getWeightPercent() == null ? 0 : goal.getWeightPercent();
            for (UserStat stat : goal.getStats()) {
                if (!stat.isActive()) {
                    continue;
                }
                for (UserMission mission : stat.getMissions()) {
                    if (mission.isActive()) {
                        missions.add(new ActiveMission(mission, goalTypeCode, mission.getMissionType(), goalWeight));
                    }
                }
            }
        }
        return missions;
    }
}
