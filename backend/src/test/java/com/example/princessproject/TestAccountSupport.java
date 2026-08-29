package com.example.princessproject;

import com.example.princessproject.auth.dto.SignupRequest;
import com.example.princessproject.auth.model.EmailVerification;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import java.time.LocalDateTime;
import java.util.UUID;

/** Test-only helper that prepares the one-time email token required by the real signup flow. */
final class TestAccountSupport {

    private TestAccountSupport() {}

    static SignupRequest verifiedSignup(EmailVerificationRepository repository, String nickname) {
        String unique = UUID.randomUUID().toString();
        String email = nickname + "+" + unique + "@example.test";
        String token = "verified-" + unique;
        EmailVerification verification = new EmailVerification(
                email, "123456", LocalDateTime.now().plusMinutes(10));
        verification.setVerified(true);
        verification.setVerifiedToken(token);
        repository.save(verification);
        return new SignupRequest(nickname + "-" + unique.substring(0, 8), "test-password", email, token, null);
    }
}
