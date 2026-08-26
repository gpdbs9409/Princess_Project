package com.example.princessproject.project.repository;

import com.example.princessproject.project.model.ProjectStatus;
import com.example.princessproject.project.model.UserProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    Optional<UserProject> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, ProjectStatus status);

    // 참가자 리스트의 삼종세트를 한 번에 조회한다. ACTIVE 프로젝트만 제한하면 과거에 상태가
    // 변경된 기존 참가자의 온보딩 값이 사라지므로, 최신 프로젝트를 우선하도록 정렬한 뒤
    // 서비스에서 사용자별 첫 항목을 선택한다.
    List<UserProject> findByUserIdInOrderByUpdatedAtDesc(List<Long> userIds);
}
