package com.example.princessproject.aifeedback.model;

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
import java.time.ZoneId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class AiFeedback {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private UserProject project;

    private LocalDate feedbackDate;

    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType = FeedbackType.DAILY;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String summary;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String praise;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String improvement;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String tomorrow;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String cheer;

    @Column(length = 100)
    private String model;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now(SEOUL_ZONE);
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now(SEOUL_ZONE);
    }
}
