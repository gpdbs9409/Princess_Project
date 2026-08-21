package com.example.princessproject.user.repository;

import com.example.princessproject.user.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    // cohort == null -> 아직 기수 배정 전(지원자)
    List<User> findByCohortIsNullOrderByCreatedAtDesc();

    List<User> findByCohortOrderByNicknameAsc(String cohort);

    List<User> findByCohortIsNotNullOrderByCohortAscNicknameAsc();

    @Query("select distinct u.cohort from User u where u.cohort is not null order by u.cohort")
    List<String> findDistinctCohorts();
}
