package com.example.princessproject.mission;

import com.example.princessproject.mission.model.MissionDefinition;
import com.example.princessproject.mission.model.MissionType;
import com.example.princessproject.common.model.StatType;
import com.example.princessproject.mission.repository.MissionDefinitionRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * MVP missions are fixed; this seeds them once so the catalog is never empty on a fresh DB.
 */
@Component
public class MissionSeeder implements ApplicationRunner {

    private final MissionDefinitionRepository missionDefinitionRepository;

    public MissionSeeder(MissionDefinitionRepository missionDefinitionRepository) {
        this.missionDefinitionRepository = missionDefinitionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (missionDefinitionRepository.count() > 0) {
            return;
        }

        missionDefinitionRepository.saveAll(List.of(
                MissionDefinition.builder()
                        .name("운동")
                        .missionType(MissionType.DAILY)
                        .statType(StatType.PHYSICAL)
                        .assignedPoints(20)
                        .targetValue(1.0)
                        .unit("session")
                        .common(false)
                        .build(),
                MissionDefinition.builder()
                        .name("식단")
                        .missionType(MissionType.DAILY)
                        .statType(StatType.PHYSICAL)
                        .assignedPoints(10)
                        .targetValue(1.0)
                        .unit("session")
                        .common(false)
                        .build(),
                MissionDefinition.builder()
                        .name("마스크팩")
                        .missionType(MissionType.DAILY)
                        .statType(StatType.PHYSICAL)
                        .assignedPoints(10)
                        .targetValue(1.0)
                        .unit("session")
                        .common(false)
                        .build(),
                MissionDefinition.builder()
                        .name("6시 기상")
                        .missionType(MissionType.DAILY)
                        .statType(StatType.PSYCHOLOGY)
                        .assignedPoints(15)
                        .targetValue(1.0)
                        .unit("session")
                        .common(false)
                        .build(),
                MissionDefinition.builder()
                        .name("일기")
                        .missionType(MissionType.DAILY)
                        .statType(StatType.PSYCHOLOGY)
                        .assignedPoints(15)
                        .targetValue(1.0)
                        .unit("session")
                        .common(true)
                        .build()
        ));
    }
}
