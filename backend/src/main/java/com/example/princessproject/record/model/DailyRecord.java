package com.example.princessproject.record.model;

import com.example.princessproject.project.model.UserMission;
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

/**
 * One day's performance against one UserMission. target/points are snapshotted at record time
 * so editing the mission's config later doesn't change past scores.
 */
@Entity
@Table(name = "daily_records")
@Getter
@Setter
@NoArgsConstructor
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private UserProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_mission_id")
    private UserMission userMission;

    private LocalDate recordDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal inputValue;

    @Column(length = 1000)
    private String photoUrl;

    @Column(length = 1000)
    private String memo;

    /**
     * Result of the GPT-4o mini vision check against the mission's expected topic. Null means
     * the check was never run (e.g. no photo required, or the client didn't report a result).
     */
    @Column(name = "ai_verified")
    private Boolean aiVerified;

    /** 운영자가 최종적으로 인증 무효 처리했는지 여부. AI 판정과 별개다. */
    @Column(name = "admin_invalidated", nullable = false)
    private boolean adminInvalidated;

    @Column(precision = 10, scale = 2)
    private BigDecimal targetValueSnapshot;

    @Column(precision = 10, scale = 2)
    private BigDecimal assignedPointsSnapshot;

    @Column(precision = 8, scale = 4)
    private BigDecimal achievementRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal earnedScore;

    @Enumerated(EnumType.STRING)
    private ScoreType scoreType = ScoreType.MAIN;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.NOT_REQUIRED;

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
