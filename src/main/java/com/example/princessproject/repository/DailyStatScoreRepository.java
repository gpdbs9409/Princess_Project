package com.example.princessproject.repository;

import com.example.princessproject.domain.DailyStatScore;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStatScoreRepository extends JpaRepository<DailyStatScore, Long> {

    List<DailyStatScore> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyStatScore> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    void deleteByUserIdAndDate(Long userId, LocalDate date);
}
