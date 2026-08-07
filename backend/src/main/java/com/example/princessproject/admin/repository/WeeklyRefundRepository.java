package com.example.princessproject.admin.repository;

import com.example.princessproject.admin.model.WeeklyRefund;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyRefundRepository extends JpaRepository<WeeklyRefund, Long> {

    Optional<WeeklyRefund> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    List<WeeklyRefund> findByWeekStart(LocalDate weekStart);
}
