package com.example.princessproject.goal.model;

import com.example.princessproject.user.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_goals")
@Getter
@Setter
@NoArgsConstructor
public class UserGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String goalHuman;

    private String goalEnding;

    public UserGoal(User user, String goalHuman, String goalEnding) {
        this.user = user;
        this.goalHuman = goalHuman;
        this.goalEnding = goalEnding;
    }
}
