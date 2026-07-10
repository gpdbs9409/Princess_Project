package com.example.princessproject.project.repository;

import com.example.princessproject.project.model.UserStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatRepository extends JpaRepository<UserStat, Long> {
}
