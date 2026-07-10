package com.example.princessproject.catalog.controller;

import com.example.princessproject.catalog.dto.GoalTypeResponse;
import com.example.princessproject.catalog.dto.MissionDefinitionResponse;
import com.example.princessproject.catalog.dto.StatTypeResponse;
import com.example.princessproject.catalog.model.GoalType;
import com.example.princessproject.catalog.model.StatType;
import com.example.princessproject.catalog.repository.GoalTypeRepository;
import com.example.princessproject.catalog.repository.MissionDefinitionRepository;
import com.example.princessproject.catalog.repository.StatTypeRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only reference data: the fixed 7 habitus, their behavior categories, and the missions
 * under each. Returned as one nested tree since the onboarding wizard needs the whole thing
 * at once to let a user pick habitus -> stats -> missions.
 */
@RestController
public class CatalogController {

    private final GoalTypeRepository goalTypeRepository;
    private final StatTypeRepository statTypeRepository;
    private final MissionDefinitionRepository missionDefinitionRepository;

    public CatalogController(
            GoalTypeRepository goalTypeRepository,
            StatTypeRepository statTypeRepository,
            MissionDefinitionRepository missionDefinitionRepository
    ) {
        this.goalTypeRepository = goalTypeRepository;
        this.statTypeRepository = statTypeRepository;
        this.missionDefinitionRepository = missionDefinitionRepository;
    }

    @GetMapping("/api/catalog")
    public List<GoalTypeResponse> getCatalog() {
        return goalTypeRepository.findByActiveTrueOrderByDisplayOrder().stream()
                .map(this::toGoalTypeResponse)
                .toList();
    }

    private GoalTypeResponse toGoalTypeResponse(GoalType goalType) {
        List<StatTypeResponse> stats = statTypeRepository
                .findByGoalTypeIdAndActiveTrueOrderByDisplayOrder(goalType.getId()).stream()
                .map(this::toStatTypeResponse)
                .toList();
        return GoalTypeResponse.from(goalType, stats);
    }

    private StatTypeResponse toStatTypeResponse(StatType statType) {
        List<MissionDefinitionResponse> missions = missionDefinitionRepository
                .findByStatTypeIdAndActiveTrueOrderById(statType.getId()).stream()
                .map(MissionDefinitionResponse::from)
                .toList();
        return StatTypeResponse.from(statType, missions);
    }
}
