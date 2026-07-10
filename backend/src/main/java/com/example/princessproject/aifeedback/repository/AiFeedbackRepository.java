package com.example.princessproject.aifeedback.repository;

import com.example.princessproject.aifeedback.model.AiFeedback;
import com.example.princessproject.aifeedback.model.FeedbackType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findByUserIdAndProjectIdAndFeedbackDateAndFeedbackType(
            Long userId, Long projectId, LocalDate feedbackDate, FeedbackType feedbackType);
}
