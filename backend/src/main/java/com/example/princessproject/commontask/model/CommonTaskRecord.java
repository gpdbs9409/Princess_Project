package com.example.princessproject.commontask.model;

import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One day's (READING/STUDY) or one week's (WEEKLY_RETROSPECTIVE) entry for a common task.
 * recordDate means "the day" for READING/STUDY and "that week's Monday" for
 * WEEKLY_RETROSPECTIVE - CommonTaskService.normalizeDate is the single place that mapping
 * happens, so callers never have to think about it.
 *
 * One table for all 3 types (rather than 3 separate tables) because they share the same
 * identity shape (one row per user per type per day/week) and the type-specific columns are
 * few enough that nullable columns are simpler than a join.
 */
@Entity
@Table(name = "common_task_records")
@Getter
@Setter
@NoArgsConstructor
public class CommonTaskRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private UserProject project;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", length = 30, nullable = false)
    private CommonTaskType taskType;

    /** The day (READING/STUDY) or that week's Monday (WEEKLY_RETROSPECTIVE). */
    private LocalDate recordDate;

    // ---- READING: 책 제목(선택), 시작~종료 페이지 ----
    @Column(name = "book_title", length = 200)
    private String bookTitle;

    private Integer startPage;
    private Integer endPage;

    // ---- STUDY: 이번 주 계획량 / 오늘 완료량 ----
    @Column(precision = 10, scale = 2)
    private BigDecimal studyPlannedAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal studyCompletedAmount;

    // ---- WEEKLY_RETROSPECTIVE: PART1/2/3 ----
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String retroDailyLife;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String retroWeekReview;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String retroNextWeekPlan;

    // READING/STUDY 전용 사진 인증 (2026-08-21: 타 습관 카드와 동일하게 사진인증 추가).
    // WEEKLY_RETROSPECTIVE에는 쓰이지 않는다 - MissionCard와 마찬가지로 업로드 URL만 들고
    // 있고, 실제 파일은 /api/uploads가 관리한다.
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 1000)
    private String memo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
