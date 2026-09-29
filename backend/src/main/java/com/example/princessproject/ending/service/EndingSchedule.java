package com.example.princessproject.ending.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 공주 엔딩 공개/운영 종료 일정 (화면설계서 v0.2, 2026-09-27).
 *
 * 모든 시각은 KST 기준이며 서버 시간으로 판단한다 (사용자 기기 시계를 믿지 않는다).
 * 기본값은 프린세스 프로젝트 1기 일정이고, 환경변수로 덮어쓸 수 있다.
 *   - 산정 기간: 1~4주차 9/1 ~ 9/27 (잔여일 9/28~9/30은 산정·그래프 모두 제외)
 *   - 공개: 10/1 00:00 ~ 10/20 23:59
 *   - 운영 종료: 10/21 00:00 부터 로그인 불가
 */
@Component
public class EndingSchedule {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDateTime revealAt;
    private final LocalDateTime closeAt;
    private final Clock clock;

    @Autowired
    public EndingSchedule(
            @Value("${ending.period-start:2026-09-01}") String periodStart,
            @Value("${ending.period-end:2026-09-27}") String periodEnd,
            @Value("${ending.reveal-at:2026-10-01T00:00:00}") String revealAt,
            @Value("${ending.close-at:2026-10-21T00:00:00}") String closeAt
    ) {
        this(LocalDate.parse(periodStart), LocalDate.parse(periodEnd),
                LocalDateTime.parse(revealAt), LocalDateTime.parse(closeAt), Clock.system(SEOUL));
    }

    public EndingSchedule(
            LocalDate periodStart, LocalDate periodEnd, LocalDateTime revealAt, LocalDateTime closeAt, Clock clock
    ) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.revealAt = revealAt;
        this.closeAt = closeAt;
        this.clock = clock;
    }

    public ZonedDateTime now() {
        return ZonedDateTime.now(clock.withZone(SEOUL));
    }

    public EndingPhase phase() {
        LocalDateTime now = now().toLocalDateTime();
        if (!now.isBefore(closeAt)) return EndingPhase.CLOSED;
        if (!now.isBefore(revealAt)) return EndingPhase.REVEALED;
        return EndingPhase.BEFORE_REVEAL;
    }

    public boolean isClosed() {
        return phase() == EndingPhase.CLOSED;
    }

    public LocalDate periodStart() {
        return periodStart;
    }

    public LocalDate periodEnd() {
        return periodEnd;
    }

    public ZonedDateTime revealAt() {
        return revealAt.atZone(SEOUL);
    }

    public ZonedDateTime closeAt() {
        return closeAt.atZone(SEOUL);
    }
}
