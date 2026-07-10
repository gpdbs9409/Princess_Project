package com.example.princessproject.repository;

import com.example.princessproject.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);
}
