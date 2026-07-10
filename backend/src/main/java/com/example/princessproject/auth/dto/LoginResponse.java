package com.example.princessproject.auth.dto;

import com.example.princessproject.user.dto.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}
