package com.example.princessproject.record.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordRequest(
        @NotNull Long userMissionId,
        @NotNull LocalDate date,
        @NotNull @DecimalMin(value = "0.0", message = "입력값은 0 이상이어야 해요") BigDecimal inputValue,
        String photoUrl,
        String memo,
        Boolean aiVerified
) {
}
