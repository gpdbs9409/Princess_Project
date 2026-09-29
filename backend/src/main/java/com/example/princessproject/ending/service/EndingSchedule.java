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

    /**
     * dev 조기 공개 (2026-09-29 요청): Railway가 자동으로 넣어주는 RAILWAY_ENVIRONMENT_NAME이
     * ending.early-reveal-environments 목록(기본 "dev")에 있으면 공개 시각을 (ending.early-reveal-at, 기본 9/29 00:00)으로
     * 앞당겨 지금 바로 엔딩을 확인할 수 있게 한다. Railway 환경 이름이 다를 경우를 대비해
     * RAILWAY_PUBLIC_DOMAIN이 backend-dev로 시작해도 dev로 본다. production 환경 이름은 목록에 없으므로 운영은
     * 항상 10/1 00:00 공개 그대로다. 운영 종료(close-at) 시각은 어느 환경이든 바꾸지 않는다.
     */
    @Autowired
    public EndingSchedule(
            @Value("${ending.period-start:2026-09-01}") String periodStart,
            @Value("${ending.period-end:2026-09-27}") String periodEnd,
            @Value("${ending.reveal-at:2026-10-01T00:00:00}") String revealAt,
            @Value("${ending.close-at:2026-10-21T00:00:00}") String closeAt,
            @Value("${RAILWAY_ENVIRONMENT_NAME:}") String environmentName,
            @Value("${RAILWAY_PUBLIC_DOMAIN:}") String publicDomain,
            @Value("${ending.early-reveal-environments:dev}") String earlyRevealEnvironments,
            @Value("${ending.early-reveal-at:2026-09-29T00:00:00}") String earlyRevealAt
    ) {
        this(LocalDate.parse(periodStart), LocalDate.parse(periodEnd),
                isEarlyReveal(environmentName, earlyRevealEnvironments) || isDevDomain(publicDomain)
                        ? LocalDateTime.parse(earlyRevealAt)
                        : LocalDateTime.parse(revealAt),
                LocalDateTime.parse(closeAt), Clock.system(SEOUL));
    }

    /** dev 백엔드 도메인(backend-dev-xxxx.up.railway.app) 보조 판별. 운영은 backend-production-xxxx. */
    static boolean isDevDomain(String publicDomain) {
        return publicDomain != null && publicDomain.trim().toLowerCase().startsWith("backend-dev");
    }

    static boolean isEarlyReveal(String environmentName, String earlyRevealEnvironments) {
        if (environmentName == null || environmentName.isBlank() || earlyRevealEnvironments == null) {
            return false;
        }
        for (String name : earlyRevealEnvironments.split(",")) {
            if (!name.isBlank() && name.trim().equalsIgnoreCase(environmentName.trim())) return true;
        }
        return false;
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
