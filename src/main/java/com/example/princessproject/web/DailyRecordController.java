package com.example.princessproject.web;

import com.example.princessproject.service.DailyRecordService;
import com.example.princessproject.service.MissionProgress;
import com.example.princessproject.service.ai.AiFeedbackResult;
import com.example.princessproject.service.ai.AiFeedbackService;
import com.example.princessproject.web.dto.AiFeedbackResponse;
import com.example.princessproject.web.dto.DailySummaryResponse;
import com.example.princessproject.web.dto.RecordRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;
    private final AiFeedbackService aiFeedbackService;

    public DailyRecordController(DailyRecordService dailyRecordService, AiFeedbackService aiFeedbackService) {
        this.dailyRecordService = dailyRecordService;
        this.aiFeedbackService = aiFeedbackService;
    }

    @PostMapping("/api/records")
    @PreAuthorize("#request.userId() == authentication.principal")
    public DailySummaryResponse saveRecord(@Valid @RequestBody RecordRequest request) {
        MissionProgress progress = dailyRecordService.saveRecord(
                request.userId(), request.missionId(), request.date(), request.inputValue(), request.photoUrl(), request.memo());
        AiFeedbackResult stored = aiFeedbackService.getStoredFeedback(request.userId(), request.date());
        return DailySummaryResponse.from(request.date(), progress, AiFeedbackResponse.from(stored));
    }

    @GetMapping("/api/users/{userId}/daily")
    @PreAuthorize("#userId == authentication.principal")
    public DailySummaryResponse getDailySummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
        AiFeedbackResult stored = aiFeedbackService.getStoredFeedback(userId, date);
        return DailySummaryResponse.from(date, progress, AiFeedbackResponse.from(stored));
    }

    @PostMapping("/api/users/{userId}/ai-feedback")
    @PreAuthorize("#userId == authentication.principal")
    public AiFeedbackResponse generateAiFeedback(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AiFeedbackResult result = aiFeedbackService.generateFeedback(userId, date);
        return AiFeedbackResponse.from(result);
    }
}
