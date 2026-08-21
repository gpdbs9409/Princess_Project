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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProjectService {

    // Mirrors chk_user_goals_weight (1-100) - checked here too so a bad value gets a specific,
    // actionable error code instead of surfacing as a generic 500 from the DB constraint.
    private static final int MIN_GOAL_WEIGHT_PERCENT = 1;
    private static final int MAX_GOAL_WEIGHT_PERCENT = 100;

    // A mission's target has to be a small positive number in practice (e.g. "30분", "10000걸음") -
    // this just guards against obviously-bogus setup input (0, negative, or something like
    // 999999) slipping through and producing a target nobody could ever complete or a
    // constraint violation at save time. Generous on purpose: not trying to guess a "correct"
    // value per mission, just rejecting values nobody could have meant to type.
    private static final BigDecimal MIN_MISSION_TARGET = new BigDecimal("0.01");
    private static final BigDecimal MAX_MISSION_TARGET = new BigDecimal("100000");
    private static final BigDecimal MAX_ASSIGNED_POINTS = new BigDecimal("100000");

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
     *
     * Not readOnly: getOrCreateActive() inserts a new project on a user's very first call, and a
     * readOnly transaction puts the JDBC connection itself in read-only mode, which made that
     * insert fail for every brand-new user ("Connection is read-only").
     */
    @Transactional
    public ProjectResponse getActiveProjectResponse(Long userId) {
        return ProjectResponse.from(getOrCreateActive(userId));
    }

    /**
     * Clearing and re-adding project.getGoals() in one flush lets Hibernate order the new
     * INSERTs before the old orphan-removal DELETEs, so re-selecting a goal type the project
     * already had collides with uk_user_goals_project_goal_type. Flushing the clear separately
     * forces the deletes through first.
     *
     * Goals/missions are only settable once, on first setup - refund eligibility is judged
     * against the originally declared missions, so they can't be changed after the fact.
     */
    @Transactional
    public UserProject replaceSelections(Long userId, ProjectSelectionsRequest request) {
        validateSelections(request);

        UserProject project = getOrCreateActive(userId);
        if (!project.getGoals().isEmpty()) {
            throw new ProjectValidationException("GOALS_ALREADY_SET", "Goals and missions can only be set once");
        }
        project.setGoalHuman(request.goalHuman());
        project.setGoalAppearance(request.goalAppearance());
        project.setGoalEnding(request.goalEnding());
        project.getGoals().clear();
        userProjectRepository.saveAndFlush(project);

        List<UserGoal> goals = new ArrayList<>();
        for (ProjectSelectionsRequest.GoalSelection goalSelection : request.goals()) {
            GoalType goalType = goalTypeRepository.findByCode(goalSelection.goalTypeCode())
                    .orElseThrow(() -> new ProjectValidationException(
                            "UNKNOWN_GOAL_TYPE", "Unknown goal type: " + goalSelection.goalTypeCode()));

            UserGoal goal = new UserGoal(project, goalType, goalSelection.weightPercent(), goalSelection.customGoalText());
            goal.getStats().addAll(buildStats(goal, goalSelection.stats()));
            goals.add(goal);
        }
        project.getGoals().addAll(goals);

        return userProjectRepository.save(project);
    }

    /**
     * Catches the input mistakes that would otherwise surface as an opaque 500 from a DB
     * constraint (duplicate goal type -> uk_user_goals_project_goal_type, weight range ->
     * chk_user_goals_weight, target range -> chk_user_missions_target) so the frontend can show
     * a specific, actionable message instead of a silent/generic failure.
     */
    private void validateSelections(ProjectSelectionsRequest request) {
        List<ProjectSelectionsRequest.GoalSelection> selectedGoals = request.goals();
        if (selectedGoals.isEmpty()) {
            return;
        }

        long distinctGoalTypes = selectedGoals.stream().map(ProjectSelectionsRequest.GoalSelection::goalTypeCode).distinct().count();
        if (distinctGoalTypes < selectedGoals.size()) {
            throw new ProjectValidationException("DUPLICATE_GOAL_TYPE", "Duplicate goal type in selection");
        }

        for (ProjectSelectionsRequest.GoalSelection goalSelection : selectedGoals) {
            Integer weightPercent = goalSelection.weightPercent();
            if (weightPercent == null || weightPercent < MIN_GOAL_WEIGHT_PERCENT || weightPercent > MAX_GOAL_WEIGHT_PERCENT) {
                throw new ProjectValidationException(
                        "GOAL_WEIGHT_OUT_OF_RANGE",
                        "Goal weight must be between " + MIN_GOAL_WEIGHT_PERCENT + " and " + MAX_GOAL_WEIGHT_PERCENT
                                + ", got " + weightPercent);
            }
            for (ProjectSelectionsRequest.StatSelection statSelection : goalSelection.stats()) {
                for (ProjectSelectionsRequest.MissionSelection missionSelection : statSelection.missions()) {
                    validateMissionSelection(missionSelection);
                }
            }
        }

        int weightSum = selectedGoals.stream().mapToInt(ProjectSelectionsRequest.GoalSelection::weightPercent).sum();
        if (weightSum != 100) {
            throw new ProjectValidationException("WEIGHT_SUM_INVALID", "Goal weights must sum to 100, got " + weightSum);
        }
    }

    private void validateMissionSelection(ProjectSelectionsRequest.MissionSelection missionSelection) {
        BigDecimal targetValue = missionSelection.targetValue();
        if (targetValue == null
                || targetValue.compareTo(MIN_MISSION_TARGET) < 0
                || targetValue.compareTo(MAX_MISSION_TARGET) > 0) {
            throw new ProjectValidationException(
                    "MISSION_TARGET_OUT_OF_RANGE",
                    "Mission target must be between " + MIN_MISSION_TARGET + " and " + MAX_MISSION_TARGET
                            + ", got " + targetValue);
        }
        BigDecimal assignedPoints = missionSelection.assignedPoints();
        if (assignedPoints == null || assignedPoints.signum() < 0 || assignedPoints.compareTo(MAX_ASSIGNED_POINTS) > 0) {
            throw new ProjectValidationException(
                    "MISSION_POINTS_OUT_OF_RANGE",
                    "Mission points must be between 0 and " + MAX_ASSIGNED_POINTS + ", got " + assignedPoints);
        }
    }

    private List<UserStat> buildStats(UserGoal goal, List<ProjectSelectionsRequest.StatSelection> statSelections) {
        List<UserStat> stats = new ArrayList<>();
        for (ProjectSelectionsRequest.StatSelection statSelection : statSelections) {
            StatType statType = null;
            if (statSelection.statTypeId() != null) {
                statType = statTypeRepository.findById(statSelection.statTypeId())
                        .orElseThrow(() -> new ProjectValidationException(
                                "STAT_TYPE_NOT_FOUND", "Stat type not found: " + statSelection.statTypeId()));
            } else if (statSelection.customStatName() == null || statSelection.customStatName().isBlank()) {
                throw new ProjectValidationException("CUSTOM_STAT_NAME_REQUIRED", "Custom stats must have a customStatName");
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
                        .orElseThrow(() -> new ProjectValidationException(
                                "MISSION_DEFINITION_NOT_FOUND",
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
