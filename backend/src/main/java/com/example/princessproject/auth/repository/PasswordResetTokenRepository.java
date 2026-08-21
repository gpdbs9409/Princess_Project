package com.example.princessproject.auth.repository;

import com.example.princessproject.auth.model.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.user.id = :userId and t.used = false")
    void deleteByUserIdAndUsedFalse(Long userId);
}
