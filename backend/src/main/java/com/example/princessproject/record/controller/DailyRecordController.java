package com.example.princessproject.record.controller;

import com.example.princessproject.aifeedback.dto.AiFeedbackResponse;
import com.example.princessproject.aifeedback.service.AiFeedbackResult;
import com.example.princessproject.aifeedback.service.AiFeedbackService;
import com.example.princessproject.record.dto.DailySummaryResponse;
import com.example.princessproject.record.dto.RecordRequest;
import com.example.princessproject.record.service.DailyRecordService;
import com.example.princessproject.record.service.MissionProgress;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ownership is derived from the JWT principal (Authentication), never a client-supplied
 * userId - DailyRecordService.saveRecord additionally verifies the target UserMission
 * actually belongs to that user before writing.
 */
@RestController
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;
    private final AiFeedbackService aiFeedbackService;

    public DailyRecordController(DailyRecordService dailyRecordService, AiFeedbackService aiFeedbackService) {
        this.dailyRecordService = dailyRecordService;
        this.aiFeedbackService = aiFeedbackService;
    }

    @PostMapping("/api/records")
    public DailySummaryResponse saveRecord(Authentication authentication, @Valid @RequestBody RecordRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        MissionProgress progress = dailyRecordService.saveRecord(
                userId, request.userMissionId(), request.date(), request.inputValue(), request.photoUrl(),
                request.memo(), request.aiVerified());
        AiFeedbackResult stored = aiFeedbackService.getStoredFeedback(userId, request.date());
        return DailySummaryResponse.from(request.date(), progress, AiFeedbackResponse.from(stored));
    }

    @GetMapping("/api/projects/active/daily")
    public DailySummaryResponse getDailySummary(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        MissionProgress progress = dailyRecordService.getMissionProgress(userId, date);
        AiFeedbackResult stored = aiFeedbackService.getStoredFeedback(userId, date);
        return DailySummaryResponse.from(date, progress, AiFeedbackResponse.from(stored));
    }

    @PostMapping("/api/projects/active/ai-feedback")
    public AiFeedbackResponse generateAiFeedback(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        AiFeedbackResult result = aiFeedbackService.generateFeedback(userId, date);
        return AiFeedbackResponse.from(result);
    }
}
