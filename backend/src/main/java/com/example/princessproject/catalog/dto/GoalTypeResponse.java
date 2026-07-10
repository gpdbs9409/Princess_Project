package com.example.princessproject.catalog.dto;

import com.example.princessproject.catalog.model.GoalType;
import com.example.princessproject.common.model.GoalTypeCode;
import java.util.List;

public record GoalTypeResponse(
        Long id,
        GoalTypeCode code,
        String name,
        String description,
        List<StatTypeResponse> stats
) {
    public static GoalTypeResponse from(GoalType goalType, List<StatTypeResponse> stats) {
        return new GoalTypeResponse(
                goalType.getId(),
                goalType.getCode(),
                goalType.getName(),
                goalType.getDescription(),
                stats
        );
    }
}
