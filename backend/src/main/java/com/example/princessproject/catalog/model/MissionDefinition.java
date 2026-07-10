package com.example.princessproject.catalog.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A catalog mission under a behavior category (e.g. 운동 -> "운동 30분"). A user picks one of
 * these (or writes a custom mission) to create a project.model.UserMission.
 */
@Entity
@Table(name = "mission_definitions")
@Getter
@Setter
@NoArgsConstructor
public class MissionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_type_id")
    private StatType statType;

    private String name;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @Enumerated(EnumType.STRING)
    private MissionType missionType = MissionType.DAILY;

    @Column(precision = 10, scale = 2)
    private BigDecimal defaultTargetValue;

    @Column(length = 50)
    private String unit;

    @Column(precision = 10, scale = 2)
    private BigDecimal defaultAssignedPoints;

    private boolean requiresPhoto;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public MissionDefinition(
            StatType statType,
            String name,
            String description,
            BigDecimal defaultTargetValue,
            String unit,
            BigDecimal defaultAssignedPoints,
            boolean requiresPhoto
    ) {
        this.statType = statType;
        this.name = name;
        this.description = description;
        this.defaultTargetValue = defaultTargetValue;
        this.unit = unit;
        this.defaultAssignedPoints = defaultAssignedPoints;
        this.requiresPhoto = requiresPhoto;
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
