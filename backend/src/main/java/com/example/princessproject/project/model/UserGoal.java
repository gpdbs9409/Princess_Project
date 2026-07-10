package com.example.princessproject.project.model;

import com.example.princessproject.catalog.model.GoalType;
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
 * A habitus (GoalType) the user picked for a project, with the % of the project's total score
 * it's worth. Replaces the old flat UserStatFocus.
 */
@Entity
@Table(name = "user_goals")
@Getter
@Setter
@NoArgsConstructor
public class UserGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private UserProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_type_id")
    private GoalType goalType;

    private Integer priority;

    private Integer weightPercent;

    @Column(length = 500)
    private String customGoalText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "userGoal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserStat> stats = new ArrayList<>();

    public UserGoal(UserProject project, GoalType goalType, Integer weightPercent, String customGoalText) {
        this.project = project;
        this.goalType = goalType;
        this.weightPercent = weightPercent;
        this.customGoalText = customGoalText;
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
