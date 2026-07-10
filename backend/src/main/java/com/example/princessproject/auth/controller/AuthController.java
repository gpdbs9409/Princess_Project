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

    /**
     * First login for a nickname creates the account with that password; a nickname that
     * already exists must supply the matching password (401 otherwise).
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
