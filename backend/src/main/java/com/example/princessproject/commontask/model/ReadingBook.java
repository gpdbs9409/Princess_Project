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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 참가자가 지금 읽고 있는(또는 예전에 읽었던) 책 한 권 (2026-09).
 *
 * <p>병렬독서는 지원하지 않는다 - 한 사용자에게 status=ACTIVE인 행은 항상 최대 1개만 존재하고,
 * 이는 ReadingBookService가 보장한다(새 책을 등록하면 기존 ACTIVE 책을 COMPLETED로 바꾼 뒤
 * 새 책을 ACTIVE로 만든다). "이전 페이지 이어쓰기"는 이 책의 startedAt 이후에 쓰인
 * CommonTaskRecord(READING)들 중 가장 최근 것의 endPage를 기준으로 한다 - 책을 바꾸면 새
 * 책에는 그 이전 기록이 없으므로 자연스럽게 1페이지부터 다시 시작한다.
 */
@Entity
@Table(name = "reading_books")
@Getter
@Setter
@NoArgsConstructor
public class ReadingBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private UserProject project;

    @Column(length = 200, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ReadingBookStatus status = ReadingBookStatus.ACTIVE;

    private LocalDate startedAt;

    private LocalDate completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ReadingBook(User user, UserProject project, String title, LocalDate startedAt) {
        this.user = user;
        this.project = project;
        this.title = title;
        this.startedAt = startedAt;
        this.status = ReadingBookStatus.ACTIVE;
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
