package com.example.princessproject.project.model;

import com.example.princessproject.catalog.model.StatType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A behavior category (StatType) the user picked under one of their chosen habitus.
 */
@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
public class UserStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_goal_id")
    private UserGoal userGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_type_id")
    private StatType statType;

    private Integer priority;

    private Integer weightPercent;

    @Column(length = 100)
    private String customStatName;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "userStat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserMission> missions = new ArrayList<>();

    public UserStat(UserGoal userGoal, StatType statType) {
        this.userGoal = userGoal;
        this.statType = statType;
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
