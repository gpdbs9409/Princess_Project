package com.example.princessproject.admin.repository;

import com.example.princessproject.admin.model.ScoreAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreAdjustmentRepository extends JpaRepository<ScoreAdjustment, Long> {

    List<ScoreAdjustment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
