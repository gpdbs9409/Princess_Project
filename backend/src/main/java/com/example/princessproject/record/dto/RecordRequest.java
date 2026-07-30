package com.example.princessproject.record.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordRequest(
        @NotNull Long userMissionId,
        @NotNull LocalDate date,
        @NotNull BigDecimal inputValue,
        String photoUrl,
        String memo,
        Boolean aiVerified
) {
}
