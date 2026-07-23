package com.example.princessproject.project.service;

import com.example.princessproject.catalog.model.GoalType;
import com.example.princessproject.catalog.model.MissionDefinition;
import com.example.princessproject.catalog.model.StatType;
import com.example.princessproject.catalog.repository.GoalTypeRepository;
import com.example.princessproject.catalog.repository.MissionDefinitionRepository;
import com.example.princessproject.catalog.repository.StatTypeRepository;
import com.example.princessproject.project.dto.ProjectResponse;
import com.example.princessproject.project.dto.ProjectSelectionsRequest;
import com.example.princessproject.project.model.ProjectStatus;
import com.example.princessproject.project.model.UserGoal;
import com.example.princessproject.project.model.UserMission;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.model.UserStat;
import com.example.princessproject.project.repository.UserProjectRepository;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProjectService {

    private final UserProjectRepository userProjectRepository;
    private final UserRepository userRepository;
    private final GoalTypeRepository goalTypeRepository;
    private final StatTypeRepository statTypeRepository;
    private final MissionDefinitionRepository missionDefinitionRepository;

    public UserProjectService(
            UserProjectRepository userProjectRepository,
            UserRepository userRepository,
            GoalTypeRepository goalTypeRepository,
            StatTypeRepository statTypeRepository,
            MissionDefinitionRepository missionDefinitionRepository
    ) {
        this.userProjectRepository = userProjectRepository;
        this.userRepository = userRepository;
        this.goalTypeRepository = goalTypeRepository;
        this.statTypeRepository = statTypeRepository;
        this.missionDefinitionRepository = missionDefinitionRepository;
    }

    @Transactional
    public UserProject getOrCreateActive(Long userId) {
        return userProjectRepository.findFirstByUserIdAndStatusOrderByIdDesc(userId, ProjectStatus.ACTIVE)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return userProjectRepository.save(new UserProject(user, "나의 성장 프로젝트"));
                });
    }

    /**
     * With spring.jpa.open-in-view=false, the session closes when getOrCreateActive() returns -
     * mapping to ProjectResponse has to happen while that session is still open (goalType/statType/
     * missionDefinition are all lazy associations), so this stays inside its own @Transactional
     * rather than letting the controller map after the fact like it does for replaceSelections
     * (there mission/goal/stat type entities come straight out of a query in the same call, never
     * a lazy proxy, so that path never actually needed this).
     */
    @Transactional(readOnly = true)
    public ProjectResponse getActiveProjectResponse(Long userId) {
        return ProjectResponse.from(getOrCreateActive(userId));
    }

    @Transactional
    public UserProject replaceSelections(Long userId, ProjectSelectionsRequest request) {
        UserProject project = getOrCreateActive(userId);
        project.setGoalHuman(request.goalHuman());
        project.setGoalEnding(request.goalEnding());
        project.getGoals().clear();

        List<UserGoal> goals = new ArrayList<>();
        for (ProjectSelectionsRequest.GoalSelection goalSelection : request.goals()) {
            GoalType goalType = goalTypeRepository.findByCode(goalSelection.goalTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown goal type: " + goalSelection.goalTypeCode()));

            UserGoal goal = new UserGoal(project, goalType, goalSelection.weightPercent(), goalSelection.customGoalText());
            goal.getStats().addAll(buildStats(goal, goalSelection.stats()));
            goals.add(goal);
        }
        project.getGoals().addAll(goals);

        return userProjectRepository.save(project);
    }

    private List<UserStat> buildStats(UserGoal goal, List<ProjectSelectionsRequest.StatSelection> statSelections) {
        List<UserStat> stats = new ArrayList<>();
        for (ProjectSelectionsRequest.StatSelection statSelection : statSelections) {
            StatType statType = null;
            if (statSelection.statTypeId() != null) {
                statType = statTypeRepository.findById(statSelection.statTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Stat type not found: " + statSelection.statTypeId()));
            } else if (statSelection.customStatName() == null || statSelection.customStatName().isBlank()) {
                throw new IllegalArgumentException("Custom stats must have a customStatName");
            }

            UserStat stat = new UserStat(goal, statType);
            stat.setWeightPercent(statSelection.weightPercent());
            stat.setCustomStatName(statSelection.customStatName());
            stat.getMissions().addAll(buildMissions(stat, statSelection.missions()));
            stats.add(stat);
        }
        return stats;
    }

    private List<UserMission> buildMissions(UserStat stat, List<ProjectSelectionsRequest.MissionSelection> missionSelections) {
        List<UserMission> missions = new ArrayList<>();
        for (ProjectSelectionsRequest.MissionSelection missionSelection : missionSelections) {
            UserMission mission = new UserMission();
            mission.setUserStat(stat);
            if (missionSelection.missionDefinitionId() != null) {
                MissionDefinition definition = missionDefinitionRepository.findById(missionSelection.missionDefinitionId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Mission definition not found: " + missionSelection.missionDefinitionId()));
                mission.setMissionDefinition(definition);
            } else {
                mission.setCustomName(missionSelection.customName());
            }
            mission.setTargetValue(missionSelection.targetValue());
            mission.setUnit(missionSelection.unit());
            mission.setAssignedPoints(missionSelection.assignedPoints());
            mission.setMissionType(missionSelection.missionType());
            missions.add(mission);
        }
        return missions;
    }
}
