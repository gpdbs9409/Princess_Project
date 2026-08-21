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

    // 1인 1회 제한 확인용 - 이 사람이 그 기수에서 이전에 이미 어느 주차든 MVP를 받은 적이
    // 있는지 확인할 때 쓴다 (주간 MVP 정책 v1.0, 2026-08-20, 시하).
    List<WeeklyMvp> findByCohortAndUserId(String cohort, Long userId);
}
