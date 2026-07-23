package com.example.princessproject.auth.controller;

import com.example.princessproject.user.model.User;
import com.example.princessproject.user.service.UserService;
import com.example.princessproject.auth.service.JwtService;
import com.example.princessproject.auth.dto.LoginRequest;
import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/signup")
    public LoginResponse signup(@Valid @RequestBody LoginRequest request) {
        User user = userService.signup(request.nickname(), request.password());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserResponse.from(user));
    }

    /**
     * Nickname must already exist (404-equivalent NICKNAME_NOT_FOUND otherwise) and the
     * password must match (401 otherwise) - no more implicit account creation on login.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.nickname(), request.password());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserResponse.from(user));
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void onBadCredentials() {
    }
}
