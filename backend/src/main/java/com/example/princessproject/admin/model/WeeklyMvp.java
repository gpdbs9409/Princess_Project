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
