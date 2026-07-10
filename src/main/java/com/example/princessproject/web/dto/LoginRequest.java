package com.example.princessproject.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String nickname) {
}
