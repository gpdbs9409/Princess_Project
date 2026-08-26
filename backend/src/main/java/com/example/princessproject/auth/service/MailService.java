package com.example.princessproject.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring's auto-configured JavaMailSender (activated once
 * spring.mail.host/username/password are set - see application.properties). Kept separate
 * from PasswordResetService/EmailVerificationService so the SMTP/JavaMail concerns don't leak
 * into the token/code logic.
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender, @Value("${mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(String toEmail, String nickname, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("[프린세스 다이어리] 비밀번호 재설정 안내");
        message.setText(
                nickname + "님, 안녕하세요.\n\n"
                        + "아래 링크에서 새 비밀번호를 설정해주세요. 이 링크는 30분 동안만 유효합니다.\n\n"
                        + resetUrl
                        + "\n\n본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다."
        );
        mailSender.send(message);
    }

    // 회원가입 전 이메일 인증 코드 (2026-08-26 요청).
    public void sendVerificationCodeEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("[프린세스 다이어리] 이메일 인증 코드");
        message.setText(
                "아래 6자리 코드를 회원가입 화면에 입력해주세요. 이 코드는 10분 동안만 유효합니다.\n\n"
                        + code
                        + "\n\n본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다."
        );
        mailSender.send(message);
    }
}
