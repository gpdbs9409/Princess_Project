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

/** A daily READING/STUDY record. Weekly retrospectives live in a separate aggregate. */
@Entity
@Table(name = "daily_common_task_records")
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

    /** The day on which the task was performed. */
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

    @Column(name = "study_youtube_url", length = 1000)
    private String studyYoutubeUrl;

    @Column(name = "study_takeaway", length = 1000)
    private String studyTakeaway;

    // 실제 파일은 /api/uploads가 관리하고 여기에는 URL만 저장한다.
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /** Vision API relevance verdict. Null is reserved for legacy records/not-yet-checked photos. */
    @Column(name = "ai_verified")
    private Boolean aiVerified;

    @Column(name = "admin_invalidated", nullable = false)
    private boolean adminInvalidated;

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
