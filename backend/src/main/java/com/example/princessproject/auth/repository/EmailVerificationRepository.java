package com.example.princessproject.auth.repository;

import com.example.princessproject.auth.model.EmailVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailOrderByIdDesc(String email);

    Optional<EmailVerification> findByEmailAndVerifiedTokenAndVerifiedTrue(String email, String verifiedToken);

    // 코드 재요청 시, 그리고 회원가입 완료 직후 정리용 - 이메일당 항상 최신 행 하나만 유효하게 유지한다.
    @Modifying
    @Transactional
    void deleteByEmail(String email);
}
