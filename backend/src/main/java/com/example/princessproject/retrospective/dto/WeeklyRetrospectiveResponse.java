package com.example.princessproject.retrospective.dto;

import com.example.princessproject.retrospective.model.WeeklyRetrospective;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeeklyRetrospectiveResponse(Long id, LocalDate recordDate, String retroDailyLife,
        String retroWeekReview, String retroNextWeekPlan, LocalDateTime createdAt) {
    public static WeeklyRetrospectiveResponse from(WeeklyRetrospective record) {
        return new WeeklyRetrospectiveResponse(record.getId(), record.getWeekStart(), record.getRetroDailyLife(),
                record.getRetroWeekReview(), record.getRetroNextWeekPlan(), record.getCreatedAt());
    }
}
