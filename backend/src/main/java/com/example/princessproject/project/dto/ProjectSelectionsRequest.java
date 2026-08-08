package com.example.princessproject.project.dto;

import com.example.princessproject.catalog.model.MissionType;
import com.example.princessproject.common.model.GoalTypeCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk "save my whole tree" request: replaces the project's goalHuman/goalEnding and its entire
 * goals -> stats -> missions selection in one call (clear-and-rebuild, same pattern the old
 * flat stat-focus endpoint used).
 */
public record ProjectSelectionsRequest(
        String goalHuman,
        String goalAppearance,
        String goalEnding,
        @NotEmpty List<@Valid GoalSelection> goals
) {
    public record GoalSelection(
            @NotNull GoalTypeCode goalTypeCode,
            @NotNull Integer weightPercent,
            String customGoalText,
            @NotEmpty List<@Valid StatSelection> stats
    ) {
    }

    /**
     * statTypeId null means a custom stat (not in the catalog) - customStatName is required in
     * that case, mirroring how MissionSelection's missionDefinitionId/customName works.
     */
    public record StatSelection(
            Long statTypeId,
            Integer weightPercent,
            String customStatName,
            @NotEmpty List<@Valid MissionSelection> missions
    ) {
    }

    public record MissionSelection(
            Long missionDefinitionId,
            String customName,
            @NotNull BigDecimal targetValue,
            @NotNull String unit,
            @NotNull BigDecimal assignedPoints,
            @NotNull MissionType missionType
    ) {
    }
}
