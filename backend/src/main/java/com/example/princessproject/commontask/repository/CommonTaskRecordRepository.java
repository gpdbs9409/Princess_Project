package com.example.princessproject.commontask.repository;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonTaskRecordRepository extends JpaRepository<CommonTaskRecord, Long> {

    Optional<CommonTaskRecord> findByUserIdAndTaskTypeAndRecordDate(Long userId, CommonTaskType taskType, LocalDate recordDate);

    List<CommonTaskRecord> findByUserIdAndRecordDateAndTaskTypeIn(Long userId, LocalDate recordDate, List<CommonTaskType> taskTypes);
}
