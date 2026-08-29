package com.example.princessproject.retrospective.repository;

import com.example.princessproject.retrospective.model.WeeklyRetrospective;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyRetrospectiveRepository extends JpaRepository<WeeklyRetrospective, Long> {
    Optional<WeeklyRetrospective> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
    Optional<WeeklyRetrospective> findByIdAndUserId(Long id, Long userId);
    List<WeeklyRetrospective> findByUserIdAndWeekStartBeforeOrderByWeekStartDesc(Long userId, LocalDate weekStart);
    List<WeeklyRetrospective> findByUserIdOrderByWeekStartDescCreatedAtDesc(Long userId);
}
