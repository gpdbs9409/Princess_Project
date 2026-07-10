package com.example.princessproject.catalog.dto;

import com.example.princessproject.catalog.model.StatType;
import java.util.List;

public record StatTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        List<MissionDefinitionResponse> missions
) {
    public static StatTypeResponse from(StatType statType, List<MissionDefinitionResponse> missions) {
        return new StatTypeResponse(
                statType.getId(),
                statType.getCode(),
                statType.getName(),
                statType.getDescription(),
                missions
        );
    }
}
