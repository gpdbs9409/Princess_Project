package com.example.princessproject.project.controller;

import com.example.princessproject.project.dto.ProjectResponse;
import com.example.princessproject.project.dto.ProjectSelectionsRequest;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.service.UserProjectService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operates on "my own active project" only - there's no client-supplied project id in the
 * path, so ownership is enforced by construction (always derived from the JWT principal, see
 * JwtAuthenticationFilter), not by a #id == principal check.
 */
@RestController
public class ProjectController {

    private final UserProjectService userProjectService;

    public ProjectController(UserProjectService userProjectService) {
        this.userProjectService = userProjectService;
    }

    @GetMapping("/api/projects/active")
    public ProjectResponse getActive(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserProject project = userProjectService.getOrCreateActive(userId);
        return ProjectResponse.from(project);
    }

    @PutMapping("/api/projects/active/selections")
    public ProjectResponse replaceSelections(
            Authentication authentication,
            @Valid @RequestBody ProjectSelectionsRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        UserProject project = userProjectService.replaceSelections(userId, request);
        return ProjectResponse.from(project);
    }
}
