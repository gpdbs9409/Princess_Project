package com.example.princessproject.aifeedback.service;

import com.example.princessproject.aifeedback.model.AiFeedback;
import com.example.princessproject.aifeedback.model.FeedbackType;
import com.example.princessproject.aifeedback.repository.AiFeedbackRepository;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import com.example.princessproject.record.service.DailyRecordService;
import com.example.princessproject.record.service.MissionProgress;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiFeedbackService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final AiFeedbackClient aiFeedbackClient;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final UserRepository userRepository;
    private final UserProjectService userProjectService;
    private final DailyRecordService dailyRecordService;

    public AiFeedbackService(
            AiFeedbackClient aiFeedbackClient,
            AiFeedbackRepository aiFeedbackRepository,
            UserRepository userRepository,
            UserProjectService userProjectService,
            DailyRecordService dailyRecordService
    ) {
        this.aiFeedbackClient = aiFeedbackClient;
        this.aiFeedbackRepository = aiFeedbackRepository;
        this.userRepository = userRepository;
        this.userProjectService = userProjectService;
        this.dailyRecordService = dailyRecordService;
    }

    @Transactional
    public AiFeedbackResult generateFeedback(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);

        MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
        AiFeedbackContext context = toContext(progress, date, LocalDateTime.now(SEOUL_ZONE));

        AiFeedbackResult result = aiFeedbackClient.generate(context);

        // 한 날짜에도 집사에게 여러 번 말을 걸 수 있으므로 매 생성 결과를 새 채팅 묶음으로 보존한다.
        AiFeedback feedback = new AiFeedback();
        feedback.setUser(user);
        feedback.setProject(project);
        feedback.setFeedbackDate(date);
        feedback.setFeedbackType(FeedbackType.DAILY);
        feedback.setSummary(result.summary());
        feedback.setPraise(result.praise());
        feedback.setImprovement(result.improvement());
        feedback.setTomorrow(result.tomorrow());
        feedback.setCheer(result.cheer());
        feedback.setModel(aiFeedbackClient.modelName());
        aiFeedbackRepository.save(feedback);

        return result;
    }

    // Not readOnly: getOrCreateActive() inserts a new project on a user's very first call, and a
    // readOnly transaction puts the JDBC connection itself in read-only mode, which fails that
    // insert for a brand-new user who hasn't had a project created yet.
    @Transactional
    public AiFeedbackResult getStoredFeedback(Long userId, LocalDate date) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        return aiFeedbackRepository
                .findTopByUserIdAndProjectIdAndFeedbackDateAndFeedbackTypeOrderByCreatedAtDesc(
                        userId, project.getId(), date, FeedbackType.DAILY)
                .map(f -> new AiFeedbackResult(f.getSummary(), f.getPraise(), f.getImprovement(), f.getTomorrow(), f.getCheer()))
                .orElse(null);
    }

    // 레오집사 채팅(누적 히스토리) 화면용 - 지금까지 쌓인 모든 날짜의 피드백을 오래된 순으로
    // 돌려준다 (2026-08-26 요청).
    @Transactional
    public List<AiFeedback> getFeedbackHistory(Long userId) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        return aiFeedbackRepository.findByUserIdAndProjectIdAndFeedbackTypeOrderByFeedbackDateAscCreatedAtAsc(
                userId, project.getId(), FeedbackType.DAILY);
    }

    private AiFeedbackContext toContext(MissionProgress progress, LocalDate date, LocalDateTime currentDateTimeKst) {
        Map<String, BigDecimal> possibleByCapital = new LinkedHashMap<>();
        progress.missionDetails().stream()
                .filter(detail -> !detail.goalTypeCode().equalsIgnoreCase("common"))
                .forEach(detail -> possibleByCapital.merge(
                        detail.goalTypeCode().toLowerCase(), detail.assignedPoints(), BigDecimal::add));

        Map<String, AiFeedbackContext.CapitalSummary> capitals = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : possibleByCapital.entrySet()) {
            BigDecimal earned = progress.statScores().getOrDefault(entry.getKey(), BigDecimal.ZERO);
            double percent = entry.getValue().signum() == 0
                    ? 0
                    : earned.divide(entry.getValue(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
            capitals.put(entry.getKey(), new AiFeedbackContext.CapitalSummary(
                    earned.doubleValue(), entry.getValue().doubleValue(), percent));
        }

        List<AiFeedbackContext.MissionSummary> missions = progress.missionDetails().stream()
                .map(detail -> new AiFeedbackContext.MissionSummary(
                        detail.name(), detail.goalTypeCode().toLowerCase(), detail.missionType().name(),
                        detail.targetValue().doubleValue(), detail.actualValue().doubleValue(),
                        detail.assignedPoints().doubleValue(), detail.earnedScore().doubleValue(),
                        detail.achievementRate().doubleValue() * 100, detail.completed() ? "COMPLETED" : "REMAINING"))
                .toList();
        return new AiFeedbackContext(
                date,
                currentDateTimeKst,
                timePeriod(currentDateTimeKst.getHour()),
                currentDateTimeKst.getMinute() % 4,
                progress.totalScore().doubleValue(),
                progress.progress().doubleValue() * 100,
                capitals,
                missions,
                progress.completedMissions(),
                progress.remainingMissions()
        );
    }

    static String timePeriod(int hour) {
        if (hour < 6) return "DAWN_EARLY_MORNING";
        if (hour < 11) return "MORNING";
        if (hour < 18) return "MIDDAY_AFTERNOON";
        return "EVENING_NIGHT";
    }
}
