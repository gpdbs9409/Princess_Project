package com.example.princessproject.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One MVP per (cohort, week) - setting a new MVP for the same cohort/week replaces the
 * previous one (see AdminService#setMvp), it isn't additive. The actual stat bonus this is
 * supposed to grant is NOT auto-applied to scoring yet - see ScoreAdjustment, which an
 * operator uses alongside this to hand out the real points. This table is just the "who
 * won this week" record for the admin UI / a future automated bonus.
 *
 * 주간 MVP 정책 v1.0 (2026-08-20, 시하): 총 4주 동안 주당 1명, 1인 1회 제한 (AdminService#setMvp가
 * 강제함). 보상은 "최종 엔딩 등급 +1단계"인데, 이건 Score 산정식과 엔딩 등급 체계가 아직 확정되지
 * 않아서 (정책 문서 7번 항목) 여기서는 자동으로 반영하지 않는다 - 이 레코드는 "누가 몇 주차 MVP인지"
 * 만 붙잡아두고, 실제 등급 상향은 그 체계가 생긴 뒤 별도로 연결해야 한다.
 */
@Entity
@Table(name = "weekly_mvp", uniqueConstraints = @UniqueConstraint(columnNames = {"cohort", "week_start"}))
@Getter
@Setter
@NoArgsConstructor
public class WeeklyMvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String cohort;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt;

    public WeeklyMvp(Long userId, String cohort, LocalDate weekStart, String note) {
        this.userId = userId;
        this.cohort = cohort;
        this.weekStart = weekStart;
        this.note = note;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
