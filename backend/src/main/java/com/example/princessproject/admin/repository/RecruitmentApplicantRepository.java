package com.example.princessproject.admin.repository;

import com.example.princessproject.admin.model.RecruitmentApplicant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentApplicantRepository extends JpaRepository<RecruitmentApplicant, Long> {

    List<RecruitmentApplicant> findAllByOrderByCreatedAtDesc();
}
