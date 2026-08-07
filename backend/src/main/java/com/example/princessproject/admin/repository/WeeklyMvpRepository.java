package com.example.princessproject.admin.repository;

import com.example.princessproject.admin.model.WeeklyMvp;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyMvpRepository extends JpaRepository<WeeklyMvp, Long> {

    Optional<WeeklyMvp> findByCohortAndWeekStart(String cohort, LocalDate weekStart);
}
