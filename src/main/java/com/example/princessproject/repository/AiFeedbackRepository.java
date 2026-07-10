package com.example.princessproject.repository;

import com.example.princessproject.domain.AiFeedback;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findByUserIdAndDate(Long userId, LocalDate date);
}
