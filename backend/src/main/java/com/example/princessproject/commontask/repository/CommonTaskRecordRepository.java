package com.example.princessproject.commontask.repository;

import com.example.princessproject.commontask.model.CommonTaskRecord;
import com.example.princessproject.commontask.model.CommonTaskType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonTaskRecordRepository extends JpaRepository<CommonTaskRecord, Long> {

    Optional<CommonTaskRecord> findTopByUserIdAndTaskTypeAndRecordDateOrderByCreatedAtDesc(
            Long userId, CommonTaskType taskType, LocalDate recordDate);

    List<CommonTaskRecord> findByUserIdAndRecordDateAndTaskTypeInOrderByCreatedAtDesc(
            Long userId, LocalDate recordDate, List<CommonTaskType> taskTypes);

    List<CommonTaskRecord> findByUserIdAndRecordDateBetweenAndTaskTypeIn(
            Long userId, LocalDate startDate, LocalDate endDate, List<CommonTaskType> taskTypes);

    List<CommonTaskRecord> findByUserIdOrderByRecordDateDescCreatedAtDesc(Long userId);

    List<CommonTaskRecord> findByAiVerifiedFalseOrderByRecordDateDescCreatedAtDesc();

    List<CommonTaskRecord> findByAiVerifiedFalseAndAdminInvalidatedFalseOrderByRecordDateDescCreatedAtDesc();

    /** "이전 페이지 이어쓰기"용 - 지금 활성화된 책이 시작된 날짜 이후의 가장 최근 독서 기록. */
    Optional<CommonTaskRecord> findFirstByUserIdAndTaskTypeAndRecordDateGreaterThanEqualAndAdminInvalidatedFalseOrderByRecordDateDescCreatedAtDesc(
            Long userId, CommonTaskType taskType, LocalDate fromDateInclusive);
}
