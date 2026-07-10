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
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiFeedbackService {

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
        AiFeedbackContext context = toContext(progress);

        AiFeedbackResult result = aiFeedbackClient.generate(context);

        AiFeedback feedback = aiFeedbackRepository
                .findByUserIdAndProjectIdAndFeedbackDateAndFeedbackType(userId, project.getId(), date, FeedbackType.DAILY)
                .orElseGet(AiFeedback::new);
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

    @Transactional(readOnly = true)
    public AiFeedbackResult getStoredFeedback(Long userId, LocalDate date) {
        UserProject project = userProjectService.getOrCreateActive(userId);
        return aiFeedbackRepository
                .findByUserIdAndProjectIdAndFeedbackDateAndFeedbackType(userId, project.getId(), date, FeedbackType.DAILY)
                .map(f -> new AiFeedbackResult(f.getSummary(), f.getPraise(), f.getImprovement(), f.getTomorrow(), f.getCheer()))
                .orElse(null);
    }

    private AiFeedbackContext toContext(MissionProgress progress) {
        Map<String, Double> statScores = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : progress.statScores().entrySet()) {
            statScores.put(entry.getKey(), entry.getValue().doubleValue());
        }
        return new AiFeedbackContext(
                progress.totalScore().doubleValue(),
                progress.progress().doubleValue(),
                statScores,
                progress.completedMissions(),
                progress.remainingMissions()
        );
    }
}
