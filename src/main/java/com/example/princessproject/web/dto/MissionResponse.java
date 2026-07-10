package com.example.princessproject.web.dto;

import com.example.princessproject.domain.MissionDefinition;
import com.example.princessproject.domain.MissionType;
import com.example.princessproject.domain.StatType;

public record MissionResponse(
        Long id,
        String name,
        MissionType missionType,
        StatType statType,
        Integer assignedPoints,
        Double targetValue,
        String unit,
        boolean common
) {
    public static MissionResponse from(MissionDefinition mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getName(),
                mission.getMissionType(),
                mission.getStatType(),
                mission.getAssignedPoints(),
                mission.getTargetValue(),
                mission.getUnit(),
                mission.isCommon()
        );
    }
}
