package com.example.princessproject.admin.dto;

import com.example.princessproject.admin.model.RecruitmentApplicant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecruitmentApplicantResponse(
        Long id,
        String name,
        String contact,
        String note,
        String status,
        LocalDate appliedAt,
        LocalDateTime createdAt
) {
    public static RecruitmentApplicantResponse from(RecruitmentApplicant a) {
        return new RecruitmentApplicantResponse(
                a.getId(), a.getName(), a.getContact(), a.getNote(),
                a.getStatus().name(), a.getAppliedAt(), a.getCreatedAt()
        );
    }
}
