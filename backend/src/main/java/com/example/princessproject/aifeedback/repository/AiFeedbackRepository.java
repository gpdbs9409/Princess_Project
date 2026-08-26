package com.example.princessproject.aifeedback.repository;

import com.example.princessproject.aifeedback.model.AiFeedback;
import com.example.princessproject.aifeedback.model.FeedbackType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findByUserIdAndProjectIdAndFeedbackDateAndFeedbackType(
            Long userId, Long projectId, LocalDate feedbackDate, FeedbackType feedbackType);

    // 레오집사 채팅 히스토리 - 날짜 오름차순으로 받아서 프론트가 그대로 위→아래 채팅처럼 렌더링한다
    // (2026-08-26 요청: 누적형태로 쭉 볼 수 있게).
    List<AiFeedback> findByUserIdAndProjectIdAndFeedbackTypeOrderByFeedbackDateAsc(
            Long userId, Long projectId, FeedbackType feedbackType);
}
