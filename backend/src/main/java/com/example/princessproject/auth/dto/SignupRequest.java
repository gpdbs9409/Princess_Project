package com.example.princessproject.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Separate from LoginRequest because email is only relevant at signup (and only there
 * do we need @Email validation) - login still only needs nickname/password.
 * Email is optional at signup; without one, "비밀번호 찾기" simply isn't available to
 * that account until they add one later from 마이페이지.
 */
public record SignupRequest(@NotBlank String nickname, @NotBlank String password, @Email String email) {
}
