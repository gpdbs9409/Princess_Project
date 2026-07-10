package com.example.princessproject.aifeedback.repository;

import com.example.princessproject.aifeedback.model.AiFeedback;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findByUserIdAndDate(Long userId, LocalDate date);
}
