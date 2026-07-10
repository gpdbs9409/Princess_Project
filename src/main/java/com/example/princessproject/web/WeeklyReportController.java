package com.example.princessproject.web;

import com.example.princessproject.service.WeeklyReportResult;
import com.example.princessproject.service.WeeklyReportService;
import com.example.princessproject.web.dto.WeeklyReportResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @GetMapping("/api/users/{userId}/weekly-report")
    @PreAuthorize("#userId == authentication.principal")
    public WeeklyReportResponse getWeeklyReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        WeeklyReportResult result = weeklyReportService.getWeeklyReport(userId, weekStart);
        return WeeklyReportResponse.from(result);
    }
}
