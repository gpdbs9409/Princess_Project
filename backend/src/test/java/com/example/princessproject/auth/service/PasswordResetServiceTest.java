package com.example.princessproject.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.princessproject.auth.model.PasswordResetToken;
import com.example.princessproject.auth.repository.PasswordResetTokenRepository;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MailService mailService;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                mailService,
                "https://princess-project-2026.vercel.app/"
        );
    }

    @Test
    void requestResetSendsAProductionLinkWithoutDoubleSlash() {
        User user = new User("혜이드", "old-hash");
        user.setId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findByNickname("혜이드")).thenReturn(Optional.of(user));

        service.requestReset("혜이드");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        String token = tokenCaptor.getValue().getToken();
        verify(mailService).sendPasswordResetEmail(
                "user@example.com",
                "혜이드",
                "https://princess-project-2026.vercel.app/reset-password?token=" + token
        );
    }

    @Test
    void resetPasswordChangesHashAndConsumesToken() {
        User user = new User("혜이드", "old-hash");
        PasswordResetToken token = new PasswordResetToken(
                user,
                "valid-token",
                LocalDateTime.now().plusMinutes(10)
        );
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.resetPassword("valid-token", "new-password");

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }
}
