package com.example.princessproject.user.dto;

import com.example.princessproject.common.model.StatType;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.model.UserStatFocus;
import java.util.Map;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String nickname,
        String goalHuman,
        String goalEnding,
        Map<StatType, Integer> statFocus
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNickname(),
                user.getGoal() != null ? user.getGoal().getGoalHuman() : null,
                user.getGoal() != null ? user.getGoal().getGoalEnding() : null,
                toStatFocusMap(user)
        );
    }

    private static Map<StatType, Integer> toStatFocusMap(User user) {
        return user.getStatFocus().stream()
                .collect(Collectors.toMap(UserStatFocus::getStatType, UserStatFocus::getWeightPercent));
    }
}
