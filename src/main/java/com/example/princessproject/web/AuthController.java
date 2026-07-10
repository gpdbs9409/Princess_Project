package com.example.princessproject.web;

import com.example.princessproject.domain.User;
import com.example.princessproject.service.UserService;
import com.example.princessproject.service.auth.JwtService;
import com.example.princessproject.web.dto.LoginRequest;
import com.example.princessproject.web.dto.LoginResponse;
import com.example.princessproject.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findOrCreateByNickname(request.nickname());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserResponse.from(user));
    }
}
