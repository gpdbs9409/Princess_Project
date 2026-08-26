package com.example.princessproject.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원가입 전 이메일 인증용 6자리 코드. 계정이 아직 없는 상태에서 발급되므로 PasswordResetToken과
 * 달리 User FK가 없다 - email 문자열 자체가 식별자다. 인증에 성공하면 verified=true와 함께
 * 1회용 verifiedToken이 발급되고, 실제 회원가입(POST /api/auth/signup) 요청에 이 토큰을 함께
 * 실어 보내야 계정이 생성된다 (EmailVerificationService.assertVerified에서 재검증).
 */
@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String email;

    @Column(length = 6, nullable = false)
    private String code;

    @Column(name = "verified_token", length = 128)
    private String verifiedToken;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    public EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
