package com.example.princessproject.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String nickname, @NotBlank String password) {
}
