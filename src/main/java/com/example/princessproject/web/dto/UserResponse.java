package com.example.princessproject.web.dto;

import com.example.princessproject.domain.StatType;
import com.example.princessproject.domain.User;
import com.example.princessproject.domain.UserStatFocus;
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
                user.getGoalHuman(),
                user.getGoalEnding(),
                toStatFocusMap(user)
        );
    }

    private static Map<StatType, Integer> toStatFocusMap(User user) {
        return user.getStatFocus().stream()
                .collect(Collectors.toMap(UserStatFocus::getStatType, UserStatFocus::getWeightPercent));
    }
}
