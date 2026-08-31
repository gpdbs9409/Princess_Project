package com.example.princessproject.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record PaybackSheetSyncResponse(
        LocalDate weekStart,
        String weekColumn,
        int eligibleCount,
        int writtenCount,
        List<String> alreadyFilled,
        List<String> missingNicknames
) {
}
