package com.example.princessproject.service.ai;

import com.example.princessproject.domain.AiFeedback;
import com.example.princessproject.domain.StatType;
import com.example.princessproject.domain.User;
import com.example.princessproject.repository.AiFeedbackRepository;
import com.example.princessproject.repository.UserRepository;
import com.example.princessproject.service.DailyRecordService;
import com.example.princessproject.service.MissionProgress;
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
    private final DailyRecordService dailyRecordService;

    public AiFeedbackService(
            AiFeedbackClient aiFeedbackClient,
            AiFeedbackRepository aiFeedbackRepository,
            UserRepository userRepository,
            DailyRecordService dailyRecordService
    ) {
        this.aiFeedbackClient = aiFeedbackClient;
        this.aiFeedbackRepository = aiFeedbackRepository;
        this.userRepository = userRepository;
        this.dailyRecordService = dailyRecordService;
    }

    @Transactional
    public AiFeedbackResult generateFeedback(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
        AiFeedbackContext context = toContext(progress);

        AiFeedbackResult result = aiFeedbackClient.generate(context);

        AiFeedback feedback = aiFeedbackRepository.findByUserIdAndDate(userId, date).orElseGet(AiFeedback::new);
        feedback.setUser(user);
        feedback.setDate(date);
        feedback.setPrompt(context.toString());
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
        return aiFeedbackRepository.findByUserIdAndDate(userId, date)
                .map(f -> new AiFeedbackResult(f.getSummary(), f.getPraise(), f.getImprovement(), f.getTomorrow(), f.getCheer()))
                .orElse(null);
    }

    private AiFeedbackContext toContext(MissionProgress progress) {
        Map<String, Double> statScores = new LinkedHashMap<>();
        for (Map.Entry<StatType, Double> entry : progress.statScores().entrySet()) {
            statScores.put(entry.getKey().name().toLowerCase(), entry.getValue());
        }
        return new AiFeedbackContext(
                progress.totalScore(),
                progress.progress(),
                statScores,
                progress.completedMissions(),
                progress.remainingMissions()
        );
    }
}
