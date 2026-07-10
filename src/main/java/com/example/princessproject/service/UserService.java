package com.example.princessproject.service;

import com.example.princessproject.domain.StatType;
import com.example.princessproject.domain.User;
import com.example.princessproject.domain.UserStatFocus;
import com.example.princessproject.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseGet(() -> userRepository.save(new User(nickname)));
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Transactional
    public User updateStatFocus(Long userId, Map<StatType, Integer> weightsByStat) {
        User user = getById(userId);
        user.getStatFocus().clear();
        List<UserStatFocus> focusList = weightsByStat.entrySet().stream()
                .map(e -> new UserStatFocus(user, e.getKey(), e.getValue()))
                .toList();
        user.getStatFocus().addAll(focusList);
        return userRepository.save(user);
    }
}
