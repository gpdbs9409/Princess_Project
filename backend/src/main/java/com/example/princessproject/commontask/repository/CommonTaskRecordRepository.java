package com.example.princessproject.commontask.repository;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonTaskRecordRepository extends JpaRepository<CommonTaskRecord, Long> {

    Optional<CommonTaskRecord> findByUserIdAndTaskTypeAndRecordDate(Long userId, CommonTaskType taskType, LocalDate recordDate);

    Optional<CommonTaskRecord> findTopByUserIdAndTaskTypeAndRecordDateOrderByCreatedAtDesc(
            Long userId, CommonTaskType taskType, LocalDate recordDate);

    Optional<CommonTaskRecord> findByIdAndUserIdAndTaskType(Long id, Long userId, CommonTaskType taskType);

    List<CommonTaskRecord> findByUserIdAndRecordDateAndTaskTypeIn(Long userId, LocalDate recordDate, List<CommonTaskType> taskTypes);

    List<CommonTaskRecord> findByUserIdAndRecordDateBetweenAndTaskTypeIn(
            Long userId, LocalDate startDate, LocalDate endDate, List<CommonTaskType> taskTypes);

    // 지난 주간회고 히스토리 (2026-08-27 요청: "그 아래에 시간 내림차순으로 지난회고쌓이게") - 이번 주는
    // getWeekly로 따로 조회하므로, 여기서는 그 주(recordDate) 이전 것만 최신순으로 가져온다.
    List<CommonTaskRecord> findByUserIdAndTaskTypeOrderByCreatedAtDesc(Long userId, CommonTaskType taskType);

    List<CommonTaskRecord> findByUserIdOrderByRecordDateDescCreatedAtDesc(Long userId);

    List<CommonTaskRecord> findByAiVerifiedFalseOrderByRecordDateDescCreatedAtDesc();
}
