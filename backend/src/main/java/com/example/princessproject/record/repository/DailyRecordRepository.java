package com.example.princessproject.record.repository;

import com.example.princessproject.record.model.DailyRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    Optional<DailyRecord> findByUserIdAndUserMissionIdAndRecordDate(Long userId, Long userMissionId, LocalDate recordDate);

    List<DailyRecord> findByUserIdAndRecordDateBetween(Long userId, LocalDate start, LocalDate end);

    long countByUserId(Long userId);
}
