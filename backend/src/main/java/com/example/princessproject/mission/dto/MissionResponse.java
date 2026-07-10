package com.example.princessproject.mission.dto;

import com.example.princessproject.mission.model.MissionDefinition;
import com.example.princessproject.mission.model.MissionType;
import com.example.princessproject.common.model.StatType;

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
