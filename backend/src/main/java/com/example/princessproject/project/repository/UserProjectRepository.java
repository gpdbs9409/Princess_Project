package com.example.princessproject.project.repository;

import com.example.princessproject.project.model.ProjectStatus;
import com.example.princessproject.project.model.UserProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    Optional<UserProject> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, ProjectStatus status);

    // 참가자 리스트에 추구미/이상향을 같이 보여주기 위한 배치 조회 (2026-08-27 요청) - 참가자
    // 수만큼 쿼리를 따로 날리는 N+1을 피하려고 한 번에 가져온다.
    List<UserProject> findByUserIdInAndStatus(List<Long> userIds, ProjectStatus status);
}
