package com.example.princessproject.user.controller;

import com.example.princessproject.upload.service.FileStorageClient;
import com.example.princessproject.user.dto.ParticipantResponse;
import com.example.princessproject.user.dto.UpdateInstagramRequest;
import com.example.princessproject.user.service.UserService;
import com.example.princessproject.user.dto.ProfileStatsResponse;
import com.example.princessproject.user.dto.UserResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * User creation happens exclusively through AuthController.login - an authenticated,
 * token-issuing endpoint - so there is no unauthenticated user-creation path here.
 * Habitus/behavior/mission selection lives under ProjectController now.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final FileStorageClient fileStorageClient;

    public UserController(UserService userService, FileStorageClient fileStorageClient) {
        this.userService = userService;
        this.fileStorageClient = fileStorageClient;
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(userService.getById(id));
    }

    @PutMapping("/{id}/profile-image")
    @PreAuthorize("#id == authentication.principal")
    public UserResponse updateProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        String url = fileStorageClient.store(file);
        return UserResponse.from(userService.updateProfileImage(id, url));
    }

    @PutMapping("/{id}/instagram")
    @PreAuthorize("#id == authentication.principal")
    public UserResponse updateInstagram(@PathVariable Long id, @RequestBody UpdateInstagramRequest request) {
        return UserResponse.from(userService.updateInstagram(id, request.instagram()));
    }

    @GetMapping("/{id}/profile-stats")
    @PreAuthorize("#id == authentication.principal")
    public ProfileStatsResponse getProfileStats(@PathVariable Long id) {
        return userService.getProfileStats(id);
    }

    // 대시보드 팔로워/팔로잉 클릭 시 "다른 참가자" 프로필 목록 (2026-08 요청). 실제 팔로우
    // 관계는 없어서 같은 기수 참가자를 그냥 보여준다 - UserService.getParticipants 참고.
    @GetMapping("/{id}/participants")
    @PreAuthorize("#id == authentication.principal")
    public List<ParticipantResponse> getParticipants(@PathVariable Long id) {
        return userService.getParticipants(id);
    }
}
