package com.example.princessproject.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecordRequest(
        @NotNull Long userId,
        @NotNull Long missionId,
        @NotNull LocalDate date,
        @NotNull Double inputValue,
        String photoUrl,
        String memo
) {
}
