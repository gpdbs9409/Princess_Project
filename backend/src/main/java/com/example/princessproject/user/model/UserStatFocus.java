package com.example.princessproject.user.model;

import com.example.princessproject.common.model.StatType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_stat_focus")
@Getter
@Setter
@NoArgsConstructor
public class UserStatFocus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private StatType statType;

    private Integer weightPercent;

    public UserStatFocus(User user, StatType statType, Integer weightPercent) {
        this.user = user;
        this.statType = statType;
        this.weightPercent = weightPercent;
    }
}
