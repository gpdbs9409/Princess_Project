package com.example.princessproject.user.controller;

import com.example.princessproject.user.service.UserService;
import com.example.princessproject.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User creation happens exclusively through AuthController.login - an authenticated,
 * token-issuing endpoint - so there is no unauthenticated user-creation path here.
 * Habitus/behavior/mission selection lives under ProjectController now.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(userService.getById(id));
    }
}
