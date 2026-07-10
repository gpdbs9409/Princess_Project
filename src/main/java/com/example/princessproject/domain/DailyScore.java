package com.example.princessproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MVP has no distinct "behavior" grouping table, so missionScore, behaviorScore and
 * statScore end up numerically identical here (see plan's flagged assumption).
 */
@Entity
@Table(name = "daily_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;

    private Double missionScore;

    private Double behaviorScore;

    private Double statScore;

    private Double bonusScore;

    private Double totalScore;

    private Double progressPercent;
}
