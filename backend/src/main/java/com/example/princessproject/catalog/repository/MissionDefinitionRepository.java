package com.example.princessproject.catalog.repository;

import com.example.princessproject.catalog.model.MissionDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionDefinitionRepository extends JpaRepository<MissionDefinition, Long> {

    List<MissionDefinition> findByStatTypeIdAndActiveTrueOrderById(Long statTypeId);

    List<MissionDefinition> findByActiveTrueOrderById();
}
