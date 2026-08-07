package com.example.princessproject.admin.dto;

import com.example.princessproject.admin.model.RecruitmentApplicant.Status;
import java.time.LocalDate;

public record RecruitmentApplicantRequest(
        String name,
        String contact,
        String note,
        Status status,
        LocalDate appliedAt
) {
}
