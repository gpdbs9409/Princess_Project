package com.example.princessproject.ending.dto;

import java.time.OffsetDateTime;

/** 화면 분기용 서버 기준 시각/구간. phase: BEFORE_REVEAL | REVEALED | CLOSED */
public record EndingStatusResponse(
        String phase,
        OffsetDateTime serverNow,
        OffsetDateTime revealAt,
        OffsetDateTime closeAt,
        boolean preview
) {
}
