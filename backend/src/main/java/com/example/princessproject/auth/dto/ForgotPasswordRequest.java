package com.example.princessproject.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String nickname) {
}
