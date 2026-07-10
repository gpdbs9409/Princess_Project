package com.example.princessproject.project.dto;

import com.example.princessproject.project.model.ProjectStatus;
import com.example.princessproject.project.model.UserGoal;
import com.example.princessproject.project.model.UserMission;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.model.UserStat;
import java.math.BigDecimal;
import java.util.Comparator;

public record ProjectResponse(
        Long id,
        String title,
        String goalHuman,
        String goalEnding,
        ProjectStatus status,
        java.util.List<GoalItem> goals
) {
    public record MissionItem(
            Long id,
            String name,
            BigDecimal targetValue,
            String unit,
            BigDecimal assignedPoints,
            boolean requiresPhoto
    ) {
        static MissionItem from(UserMission mission) {
            boolean requiresPhoto = mission.getMissionDefinition() != null
                    && mission.getMissionDefinition().isRequiresPhoto();
            return new MissionItem(
                    mission.getId(),
                    mission.displayName(),
                    mission.getTargetValue(),
                    mission.getUnit(),
                    mission.getAssignedPoints(),
                    requiresPhoto
            );
        }
    }

    public record StatItem(
            Long id,
            Long statTypeId,
            String name,
            Integer weightPercent,
            java.util.List<MissionItem> missions
    ) {
        static StatItem from(UserStat stat) {
            String name = stat.getCustomStatName() != null
                    ? stat.getCustomStatName()
                    : stat.getStatType().getName();
            return new StatItem(
                    stat.getId(),
                    stat.getStatType().getId(),
                    name,
                    stat.getWeightPercent(),
                    stat.getMissions().stream()
                            .sorted(Comparator.comparing(UserMission::getId))
                            .map(MissionItem::from)
                            .toList()
            );
        }
    }

    public record GoalItem(
            Long id,
            String goalTypeCode,
            String name,
            Integer weightPercent,
            java.util.List<StatItem> stats
    ) {
        static GoalItem from(UserGoal goal) {
            return new GoalItem(
                    goal.getId(),
                    goal.getGoalType().getCode().name(),
                    goal.getGoalType().getName(),
                    goal.getWeightPercent(),
                    goal.getStats().stream()
                            .sorted(Comparator.comparing(UserStat::getId))
                            .map(StatItem::from)
                            .toList()
            );
        }
    }

    public static ProjectResponse from(UserProject project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getGoalHuman(),
                project.getGoalEnding(),
                project.getStatus(),
                project.getGoals().stream()
                        .sorted(Comparator.comparing(UserGoal::getId))
                        .map(GoalItem::from)
                        .toList()
        );
    }
}
