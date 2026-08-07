package com.example.princessproject.admin.dto;

import com.example.princessproject.user.model.User;

public record AdminMemberResponse(Long userId, String nickname, String cohort) {
    public static AdminMemberResponse from(User user) {
        return new AdminMemberResponse(user.getId(), user.getNickname(), user.getCohort());
    }
}
