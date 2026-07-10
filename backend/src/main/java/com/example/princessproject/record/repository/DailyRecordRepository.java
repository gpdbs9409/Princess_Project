package com.example.princessproject.record.repository;

import com.example.princessproject.record.model.DailyRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    List<DailyRecord> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyRecord> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    Optional<DailyRecord> findByUserIdAndMissionIdAndDate(Long userId, Long missionId, LocalDate date);
}
