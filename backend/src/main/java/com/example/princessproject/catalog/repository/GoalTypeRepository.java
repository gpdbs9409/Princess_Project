package com.example.princessproject.catalog.repository;

import com.example.princessproject.catalog.model.GoalType;
import com.example.princessproject.common.model.GoalTypeCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalTypeRepository extends JpaRepository<GoalType, Long> {

    Optional<GoalType> findByCode(GoalTypeCode code);

    List<GoalType> findByActiveTrueOrderByDisplayOrder();
}
