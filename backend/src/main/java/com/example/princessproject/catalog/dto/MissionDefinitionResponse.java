package com.example.princessproject.catalog.dto;

import com.example.princessproject.catalog.model.MissionDefinition;
import com.example.princessproject.catalog.model.MissionType;
import java.math.BigDecimal;

public record MissionDefinitionResponse(
        Long id,
        String name,
        String description,
        MissionType missionType,
        BigDecimal defaultTargetValue,
        String unit,
        BigDecimal defaultAssignedPoints,
        boolean requiresPhoto
) {
    public static MissionDefinitionResponse from(MissionDefinition mission) {
        return new MissionDefinitionResponse(
                mission.getId(),
                mission.getName(),
                mission.getDescription(),
                mission.getMissionType(),
                mission.getDefaultTargetValue(),
                mission.getUnit(),
                mission.getDefaultAssignedPoints(),
                mission.isRequiresPhoto()
        );
    }
}
