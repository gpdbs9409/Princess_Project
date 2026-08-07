package com.example.princessproject.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal-only recruitment bookkeeping - "누가 지원했는지" - kept entirely separate from
 * the `users` table on purpose. The app URL is only ever handed out to people who already
 * cleared this list, so every actual signup (User) is already a real participant; there is
 * no in-app "applicant waiting for approval" state. This table exists purely so the team has
 * somewhere to log applicants before/outside of the app itself. Manual entry only for now -
 * an Excel import is a possible follow-up, not built yet.
 */
@Entity
@Table(name = "recruitment_applicants")
@Getter
@Setter
@NoArgsConstructor
public class RecruitmentApplicant {

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // 연락처(전화/인스타/이메일 등) - 형식을 강제하지 않는다
    @Column(length = 200)
    private String contact;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    private LocalDate appliedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public RecruitmentApplicant(String name, String contact, String note, Status status, LocalDate appliedAt) {
        this.name = name;
        this.contact = contact;
        this.note = note;
        this.status = status != null ? status : Status.PENDING;
        this.appliedAt = appliedAt;
    }

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
