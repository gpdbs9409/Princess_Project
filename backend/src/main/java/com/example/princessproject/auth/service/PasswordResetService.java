package com.example.princessproject.auth.service;

import com.example.princessproject.auth.model.PasswordResetToken;
import com.example.princessproject.auth.repository.PasswordResetTokenRepository;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final int TOKEN_VALID_MINUTES = 30;
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Looks up by nickname (what the user actually remembers, not email) and requires an
     * email to already be on file - there's no "type your email" fallback here, because we
     * never want to email a password-reset link to an address the account owner never
     * actually confirmed belongs to their account.
     */
    @Transactional
    public void requestReset(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new AuthValidationException("NICKNAME_NOT_FOUND", "No such nickname: " + nickname));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new AuthValidationException("EMAIL_NOT_SET", "No email registered for this account");
        }

        tokenRepository.deleteByUserIdAndUsedFalse(user.getId());

        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken resetToken = new PasswordResetToken(
                user, rawToken, LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES));
        tokenRepository.save(resetToken);

        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
        mailService.sendPasswordResetEmail(user.getEmail(), user.getNickname(), resetUrl);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new AuthValidationException("TOKEN_INVALID", "Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new AuthValidationException("TOKEN_ALREADY_USED", "This reset link was already used");
        }
        if (!resetToken.isValid()) {
            throw new AuthValidationException("TOKEN_EXPIRED", "This reset link has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
