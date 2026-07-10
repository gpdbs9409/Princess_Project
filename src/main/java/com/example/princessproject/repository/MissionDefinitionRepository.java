package com.example.princessproject.repository;

import com.example.princessproject.domain.MissionDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionDefinitionRepository extends JpaRepository<MissionDefinition, Long> {

    List<MissionDefinition> findAllByOrderByIdAsc();
}
