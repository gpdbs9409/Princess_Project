package com.example.princessproject.commontask.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterReadingBookRequest(@NotBlank String title) {
}
