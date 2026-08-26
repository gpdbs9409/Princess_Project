package com.example.princessproject.user.service;

import com.example.princessproject.auth.service.AuthValidationException;
import com.example.princessproject.record.repository.DailyRecordRepository;
import com.example.princessproject.user.dto.ParticipantResponse;
import com.example.princessproject.user.dto.ProfileStatsResponse;
import com.example.princessproject.user.model.Role;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DailyRecordRepository dailyRecordRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DailyRecordRepository dailyRecordRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dailyRecordRepository = dailyRecordRepository;
    }

    /**
     * Signup and login used to be one combined "first use creates the account" flow, but that
     * silently created a brand-new empty account on any nickname typo instead of telling the
     * user their nickname was wrong - now separate, each with its own clear failure mode.
     *
     * email is optional (null/blank both treated as "not provided") since it only exists to
     * power "비밀번호 찾기" - accounts created before this feature, or anyone who skips it,
     * simply can't use password reset until they add one from 마이페이지.
     */
    @Transactional
    public User signup(String nickname, String rawPassword, String email) {
        if (userRepository.findByNickname(nickname).isPresent()) {
            throw new AuthValidationException("NICKNAME_TAKEN", "Nickname already exists: " + nickname);
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null && userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthValidationException("EMAIL_TAKEN", "Email already registered: " + normalizedEmail);
        }
        User user = new User(nickname, passwordEncoder.encode(rawPassword));
        user.setEmail(normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional
    public User authenticate(String nickname, String rawPassword) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new AuthValidationException("NICKNAME_NOT_FOUND", "No such nickname: " + nickname));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid nickname or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /**
     * The users table is the single source of truth for who is staff - login no longer
     * recomputes the role from config, so a role set here (or by hand in the DB) sticks.
     */
    @Transactional
    public User setRole(Long userId, Role role) {
        User user = getById(userId);
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfileImage(Long userId, String profileImageUrl) {
        User user = getById(userId);
        user.setProfileImageUrl(profileImageUrl);
        return userRepository.save(user);
    }

    @Transactional
    public User updateEmail(Long userId, String email) {
        User user = getById(userId);
        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new AuthValidationException("EMAIL_TAKEN", "Email already registered: " + normalizedEmail);
                });
        user.setEmail(normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public ProfileStatsResponse getProfileStats(Long userId) {
        long recordCount = dailyRecordRepository.countByUserId(userId);
        long totalUsers = userRepository.count();
        return new ProfileStatsResponse(recordCount, totalUsers);
    }

    /**
     * 대시보드 팔로워/팔로잉 클릭 시 보여줄 "다른 참가자" 목록 (2026-08 요청). 실제 팔로우
     * 관계는 없으므로, 요청한 사람과 같은 기수(cohort) 참가자를 본인 제외하고 닉네임순으로
     * 보여준다. 기수 배정 전(cohort == null)이면 아직 같이 묶일 사람이 없으니 빈 목록을 준다.
     */
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(Long userId) {
        User requester = getById(userId);
        if (requester.getCohort() == null) {
            return List.of();
        }
        return userRepository.findByCohortOrderByNicknameAsc(requester.getCohort()).stream()
                .filter(user -> !user.getId().equals(userId))
                .map(ParticipantResponse::from)
                .toList();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase();
    }
}
