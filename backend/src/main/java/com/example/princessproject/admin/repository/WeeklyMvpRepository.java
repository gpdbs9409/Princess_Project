package com.example.princessproject.admin.repository;

import com.example.princessproject.admin.model.WeeklyMvp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyMvpRepository extends JpaRepository<WeeklyMvp, Long> {

    Optional<WeeklyMvp> findByCohortAndWeekStart(String cohort, LocalDate weekStart);

    // Batch lookup for listing a whole cohort/week at once instead of one query per member.
    List<WeeklyMvp> findByWeekStart(LocalDate weekStart);
}
