package com.example.princessproject.admin.dto;

/** true면 유효 인증 승인, false면 인증 무효 처리. */
public record ActivityReviewRequest(boolean valid) {}
