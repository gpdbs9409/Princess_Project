package com.example.princessproject.record.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecordRequest(
        @NotNull Long userMissionId,
        @NotNull LocalDate date,
        @NotNull @DecimalMin(value = "0.0", message = "입력값은 0 이상이어야 해요") BigDecimal inputValue,
        String photoUrl,
        String memo,
        Boolean aiVerified,
        /** 참고용 추가 인증 사진 (최대 4장, 2026-09). 대표 사진(photoUrl)만 AI 판정 대상이다. */
        List<String> extraPhotoUrls
) {
}
