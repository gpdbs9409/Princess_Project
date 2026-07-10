package com.example.princessproject.user.controller;

import com.example.princessproject.common.model.StatType;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.service.UserService;
import com.example.princessproject.user.dto.StatFocusRequest;
import com.example.princessproject.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User creation happens exclusively through AuthController.login - an authenticated,
 * token-issuing endpoint - so there is no unauthenticated user-creation path here.
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

    @PutMapping("/{id}/stat-focus")
    @PreAuthorize("#id == authentication.principal")
    public UserResponse updateStatFocus(@PathVariable Long id, @Valid @RequestBody StatFocusRequest request) {
        Map<StatType, Integer> weightsByStat = request.stats().stream()
                .collect(Collectors.toMap(StatFocusRequest.StatFocusItem::statType, StatFocusRequest.StatFocusItem::weightPercent));
        User user = userService.updateStatFocus(id, weightsByStat);
        return UserResponse.from(user);
    }
}
