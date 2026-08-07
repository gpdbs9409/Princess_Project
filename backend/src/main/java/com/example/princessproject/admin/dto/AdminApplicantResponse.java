package com.example.princessproject.admin.dto;

import com.example.princessproject.user.model.User;
import java.time.LocalDateTime;

public record AdminApplicantResponse(
        Long userId,
        String nickname,
        LocalDateTime appliedAt
) {
    public static AdminApplicantResponse from(User user) {
        return new AdminApplicantResponse(user.getId(), user.getNickname(), user.getCreatedAt());
    }
}
