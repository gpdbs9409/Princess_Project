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
import java.util.concurrent.ThreadLocalRandom;
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

    // 의도적으로 @Transactional을 걸지 않는다 (2026-09): 이 메서드는 중간에 OpenAI 호출
    // (aiFeedbackClient.generate)이 껴 있는데, 예전에는 메서드 전체가 @Transactional이라 그
    // 외부 API 호출 몇 초 동안에도 HikariCP 커넥션을 하나 붙잡고 있었다. 동시에 여러 명이 이
    // 엔드포인트를 부르면(마감 시간대 등) 커넥션 풀(기본 10개)이 금방 바닥나서, 이 요청뿐 아니라
    // 전혀 무관한 다른 요청(기록 저장, 로그인 등)까지 커넥션을 못 받아 실패하는 게 실제 원인이었다.
    // 아래에서 실제로 DB에 닿는 호출들(dailyRecordService.getMissionProgress, aiFeedbackRepository
    // 의 조회/저장)은 전부 그 자신의 짧은 트랜잭션을 이미 가지고 있으므로(Spring Data JPA 리포지토리
    // 메서드는 기본적으로 자체 트랜잭션을 연다), 여기서 감싸지 않아도 각 단계는 원자적이다. 이
    // 메서드 전체의 원자성은 필요 없다 - 조회들은 서로 독립적이고, 마지막 save 한 번이면 충분하다.
    public AiFeedbackResult generateFeedback(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserProject project = userProjectService.getOrCreateActive(userId);

        MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
        AiFeedbackContext.PreviousFeedback previousFeedback = aiFeedbackRepository
                .findTopByUserIdAndProjectIdAndFeedbackDateAndFeedbackTypeOrderByCreatedAtDesc(
                        userId, project.getId(), date, FeedbackType.DAILY)
                .map(f -> new AiFeedbackContext.PreviousFeedback(
                        f.getSummary(), f.getPraise(), f.getImprovement(), f.getTomorrow(), f.getCheer()))
                .orElse(null);
        String responseTemplate = responseTemplate(ThreadLocalRandom.current().nextInt(4));
        AiFeedbackContext context = toContext(
                progress, date, LocalDateTime.now(SEOUL_ZONE), responseTemplate, previousFeedback);

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

    private AiFeedbackContext toContext(
            MissionProgress progress,
            LocalDate date,
            LocalDateTime currentDateTimeKst,
            String responseTemplate,
            AiFeedbackContext.PreviousFeedback previousFeedback
    ) {
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
                        detail.achievementRate().doubleValue() * 100, detail.completed() ? "COMPLETED" : "REMAINING",
                        detail.unit()))
                .toList();
        return new AiFeedbackContext(
                date,
                currentDateTimeKst,
                timePeriod(currentDateTimeKst.getHour()),
                responseTemplate,
                previousFeedback,
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

    static String responseTemplate(int variant) {
        return switch (variant) {
            case 0 -> "WARM_GREETING";
            case 1 -> "QUIET_OBSERVATION";
            case 2 -> "GENTLE_QUESTION";
            case 3 -> "BUTLER_ACTION_PLAN";
            default -> throw new IllegalArgumentException("Unknown response template: " + variant);
        };
    }
}
