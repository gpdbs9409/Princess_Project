package com.example.princessproject.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 2026-08-26: 이메일이 선택에서 필수로 바뀌었다 - "비밀번호 찾기"뿐 아니라 가입 자체가 이메일
 * 인증을 통과해야 가능해졌기 때문. emailVerificationToken은 POST /api/auth/email-verification/confirm
 * 에서 발급받은 1회용 토큰으로, UserService.signup()이 EmailVerificationService로 다시 검증한다
 * (프론트가 보낸 토큰을 그냥 믿지 않는다).
 */
public record SignupRequest(
        @NotBlank String nickname,
        @NotBlank String password,
        @NotBlank @Email String email,
        @NotBlank String emailVerificationToken
) {
}
