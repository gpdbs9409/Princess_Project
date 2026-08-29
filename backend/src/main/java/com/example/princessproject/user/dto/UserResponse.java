package com.example.princessproject.user.dto;

import com.example.princessproject.user.model.User;

public record UserResponse(
        Long id,
        String nickname,
        String email,
        String profileImageUrl,
        String role,
        String instagram
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getNickname(), user.getEmail(), user.getProfileImageUrl(), user.getRole().name(), user.getInstagram());
    }
}
