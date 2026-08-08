package com.example.princessproject.user.service;

import com.example.princessproject.auth.service.AuthValidationException;
import com.example.princessproject.record.repository.DailyRecordRepository;
import com.example.princessproject.user.dto.ProfileStatsResponse;
import com.example.princessproject.user.model.Role;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDateTime;
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
     */
    @Transactional
    public User signup(String nickname, String rawPassword) {
        if (userRepository.findByNickname(nickname).isPresent()) {
            throw new AuthValidationException("NICKNAME_TAKEN", "Nickname already exists: " + nickname);
        }
        User user = new User(nickname, passwordEncoder.encode(rawPassword));
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

    @Transactional(readOnly = true)
    public ProfileStatsResponse getProfileStats(Long userId) {
        long recordCount = dailyRecordRepository.countByUserId(userId);
        long totalUsers = userRepository.count();
        return new ProfileStatsResponse(recordCount, totalUsers);
    }
}
