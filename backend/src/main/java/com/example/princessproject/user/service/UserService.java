package com.example.princessproject.user.service;

import com.example.princessproject.record.repository.DailyRecordRepository;
import com.example.princessproject.user.dto.ProfileStatsResponse;
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
     * First login for a nickname creates the account with that password; every later login
     * with the same nickname must supply the matching password.
     */
    @Transactional
    public User authenticate(String nickname, String rawPassword) {
        User user = userRepository.findByNickname(nickname)
                .orElseGet(() -> userRepository.save(new User(nickname, passwordEncoder.encode(rawPassword))));

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
