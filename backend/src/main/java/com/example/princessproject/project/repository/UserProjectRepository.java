package com.example.princessproject.project.repository;

import com.example.princessproject.project.model.ProjectStatus;
import com.example.princessproject.project.model.UserProject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    Optional<UserProject> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, ProjectStatus status);
}
