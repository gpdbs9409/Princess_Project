package com.example.princessproject.catalog.repository;

import com.example.princessproject.catalog.model.StatType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatTypeRepository extends JpaRepository<StatType, Long> {

    Optional<StatType> findByGoalTypeIdAndCode(Long goalTypeId, String code);

    List<StatType> findByGoalTypeIdAndActiveTrueOrderByDisplayOrder(Long goalTypeId);

    List<StatType> findByActiveTrueOrderByDisplayOrder();
}
