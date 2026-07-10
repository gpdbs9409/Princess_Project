package com.example.princessproject.record.controller;

import com.example.princessproject.record.dto.WeeklyReportResponse;
import com.example.princessproject.record.service.WeeklyReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @GetMapping("/api/projects/active/weekly-report")
    public WeeklyReportResponse getWeeklyReport(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return WeeklyReportResponse.from(weeklyReportService.getWeeklyReport(userId, weekStart));
    }
}
