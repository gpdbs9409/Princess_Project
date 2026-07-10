package com.example.princessproject.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A behavior-category under a habitus (e.g. PHYSICAL -> 운동/식단/수면). Not the old flat
 * "StatType enum" - this is a real catalog row a user picks from (see project.model.UserStat).
 */
@Entity
@Table(name = "stat_types")
@Getter
@Setter
@NoArgsConstructor
public class StatType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_type_id")
    private GoalType goalType;

    @Column(length = 50)
    private String code;

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    private Integer displayOrder;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public StatType(GoalType goalType, String code, String name, String description, Integer displayOrder) {
        this.goalType = goalType;
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
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
