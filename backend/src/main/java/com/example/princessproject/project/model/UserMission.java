package com.example.princessproject.project.model;

import com.example.princessproject.catalog.model.MissionDefinition;
import com.example.princessproject.catalog.model.MissionType;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The user's actual mission instance: either picked from the MissionDefinition catalog (target/
 * unit/points copied from its defaults, editable) or fully custom (missionDefinition null,
 * customName required - see the schema's chk_user_missions_name).
 */
@Entity
@Table(name = "user_missions")
@Getter
@Setter
@NoArgsConstructor
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_stat_id")
    private UserStat userStat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_definition_id")
    private MissionDefinition missionDefinition;

    private String customName;

    @Column(precision = 10, scale = 2)
    private BigDecimal targetValue;

    @Column(length = 50)
    private String unit;

    @Column(precision = 10, scale = 2)
    private BigDecimal assignedPoints;

    @Enumerated(EnumType.STRING)
    private MissionType missionType = MissionType.DAILY;

    private Integer priority;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public String displayName() {
        return missionDefinition != null ? missionDefinition.getName() : customName;
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
