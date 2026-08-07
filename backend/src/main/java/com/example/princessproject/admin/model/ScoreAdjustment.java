package com.example.princessproject.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A manual point correction an operator makes for a member - covers both "MVP 보너스" and
 * plain complaint/dispute corrections ("실제 점수가 계산되었어도 컴플레인이 들어올 수
 * 있다"). Deliberately additive/append-only (see AdminService#deleteAdjustment for the
 * rollback path - it removes the row rather than trying to "undo" a mutation) so there's
 * always a record of who adjusted what and why, instead of silently editing a score field.
 *
 * NOTE: as of this feature's first cut, these rows are tracked and visible in the admin UI
 * but are NOT yet folded into the member-facing totals on the dashboard/weekly report -
 * that wiring is a follow-up (see project docs).
 */
@Entity
@Table(name = "score_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class ScoreAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // NULL = 특정 주가 아닌 전체/최종 보정
    @Column(name = "week_start")
    private LocalDate weekStart;

    // NULL = 특정 자본이 아닌 총점 보정 (GoalTypeCode 이름 문자열, 예: "KNOWLEDGE")
    @Column(name = "stat_type_code", length = 20)
    private String statTypeCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal points;

    @Column(length = 500)
    private String reason;

    private LocalDateTime createdAt;

    public ScoreAdjustment(Long userId, LocalDate weekStart, String statTypeCode, BigDecimal points, String reason) {
        this.userId = userId;
        this.weekStart = weekStart;
        this.statTypeCode = statTypeCode;
        this.points = points;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
