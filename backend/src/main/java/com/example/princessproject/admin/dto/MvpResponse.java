package com.example.princessproject.admin.dto;

import com.example.princessproject.admin.model.WeeklyMvp;
import java.time.LocalDate;

public record MvpResponse(Long userId, String nickname, String cohort, LocalDate weekStart, String note) {
    public static MvpResponse from(WeeklyMvp mvp, String nickname) {
        return new MvpResponse(mvp.getUserId(), nickname, mvp.getCohort(), mvp.getWeekStart(), mvp.getNote());
    }
}
