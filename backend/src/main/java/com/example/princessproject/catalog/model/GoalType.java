package com.example.princessproject.catalog.model;

import com.example.princessproject.common.model.GoalTypeCode;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catalog of the 7 fixed habitus/자본 a user can focus on. Seeded once by CatalogSeeder,
 * read-only from the application's perspective otherwise.
 */
@Entity
@Table(name = "goal_types")
@Getter
@Setter
@NoArgsConstructor
public class GoalType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private GoalTypeCode code;

    @Column(length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    private Integer displayOrder;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public GoalType(GoalTypeCode code, String name, String description, Integer displayOrder) {
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
