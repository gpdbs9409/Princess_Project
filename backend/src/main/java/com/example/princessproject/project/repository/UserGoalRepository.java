package com.example.princessproject.project.repository;

import com.example.princessproject.project.model.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {
}
