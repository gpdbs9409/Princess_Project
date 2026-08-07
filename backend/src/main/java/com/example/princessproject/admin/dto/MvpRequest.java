package com.example.princessproject.admin.dto;

import java.time.LocalDate;

public record MvpRequest(Long userId, LocalDate weekStart, String note) {
}
