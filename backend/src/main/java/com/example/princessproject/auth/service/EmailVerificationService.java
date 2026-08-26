package com.example.princessproject.auth.service;

import com.example.princessproject.auth.model.EmailVerification;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 전 이메일 인증 흐름 (2026-08-26 요청: 이메일 선택 -> 필수, 인증 통과 후에만 가입 허용).
 * PasswordResetService와 패턴은 비슷하지만("코드/토큰 발급 -> 검증" 구조, SecureRandom +
 * Base64 URL 인코딩 토큰) 대상이 "이미 있는 계정"이 아니라 "아직 계정이 없는 이메일 문자열"이라는
 * 점이 달라서 User FK 없이 email로만 식별한다.
 *
 * 코드를 재요청하면 해당 이메일의 기존 행(인증 성공 여부와 무관하게)을 전부 지운다 - 그래야
 * 이메일당 항상 최신 코드/토큰 단 하나만 유효하고, 예전에 인증에 성공했지만 가입을 완료하지 않고
 * 이탈한 경우에도 다음에는 새 코드를 다시 받아야 가입할 수 있다.
 */
@Service
public class EmailVerificationService {

    private static final int CODE_VALID_MINUTES = 10;
    private static final int TOKEN_BYTES = 24;

    private final EmailVerificationRepository repository;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(EmailVerificationRepository repository, MailService mailService) {
        this.repository = repository;
        this.mailService = mailService;
    }

    @Transactional
    public void requestCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        repository.deleteByEmail(normalizedEmail);

        String code = generateCode();
        EmailVerification verification = new EmailVerification(
                normalizedEmail, code, LocalDateTime.now().plusMinutes(CODE_VALID_MINUTES));
        repository.save(verification);

        mailService.sendVerificationCodeEmail(normalizedEmail, code);
    }

    @Transactional
    public String confirmCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        EmailVerification verification = repository.findTopByEmailOrderByIdDesc(normalizedEmail)
                .orElseThrow(() -> new AuthValidationException(
                        "CODE_NOT_REQUESTED", "No verification code requested for this email"));

        if (verification.isExpired()) {
            throw new AuthValidationException("CODE_EXPIRED", "Verification code expired");
        }
        if (!verification.getCode().equals(code)) {
            throw new AuthValidationException("CODE_INVALID", "Incorrect verification code");
        }

        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String verifiedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        verification.setVerified(true);
        verification.setVerifiedToken(verifiedToken);
        repository.save(verification);

        return verifiedToken;
    }

    /**
     * UserService.signup()이 실제 계정 생성 직전에 호출한다. 프론트가 보낸 토큰이 이 이메일로
     * 인증 완료된 것과 정확히 일치해야 통과한다.
     */
    @Transactional(readOnly = true)
    public void assertVerified(String email, String verifiedToken) {
        String normalizedEmail = normalizeEmail(email);
        if (verifiedToken == null || verifiedToken.isBlank()) {
            throw new AuthValidationException("EMAIL_NOT_VERIFIED", "Email verification required");
        }
        repository.findByEmailAndVerifiedTokenAndVerifiedTrue(normalizedEmail, verifiedToken)
                .orElseThrow(() -> new AuthValidationException("EMAIL_NOT_VERIFIED", "Email verification required"));
    }

    @Transactional
    public void clearAfterSignup(String email) {
        repository.deleteByEmail(normalizeEmail(email));
    }

    private String generateCode() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
