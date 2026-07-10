package com.example.princessproject.repository;

import com.example.princessproject.domain.DailyScore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScoreRepository extends JpaRepository<DailyScore, Long> {

    Optional<DailyScore> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyScore> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
}
