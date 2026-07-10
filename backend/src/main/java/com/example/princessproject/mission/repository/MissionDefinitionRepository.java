package com.example.princessproject.mission.repository;

import com.example.princessproject.mission.model.MissionDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionDefinitionRepository extends JpaRepository<MissionDefinition, Long> {

    List<MissionDefinition> findAllByOrderByIdAsc();
}
