package com.example.princessproject.retrospective.model;

import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "weekly_retrospectives")
@Getter @Setter @NoArgsConstructor
public class WeeklyRetrospective {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id", nullable = false)
    private UserProject project;
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) private String retroDailyLife;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) private String retroWeekReview;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) private String retroNextWeekPlan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
