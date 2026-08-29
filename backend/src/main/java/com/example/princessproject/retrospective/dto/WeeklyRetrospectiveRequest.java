package com.example.princessproject.retrospective.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record WeeklyRetrospectiveRequest(
        @NotNull LocalDate date,
        String retroDailyLife,
        String retroWeekReview,
        String retroNextWeekPlan
) {}
