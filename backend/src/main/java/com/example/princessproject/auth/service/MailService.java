package com.example.princessproject.auth.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resend(https://resend.com)의 HTTP API로 메일을 보낸다 (2026-08-27: Gmail SMTP 직통에서 전환).
 *
 * 전환 이유: dev 환경(Railway)에서 smtp.gmail.com:587로 나가는 연결이 계속
 * "Couldn't connect to host ... timeout 5000"으로 실패했다. 비밀번호 문제(앱 비밀번호 공백)를
 * 고치고, IPv4 강제(-Djava.net.preferIPv4Stack=true)까지 해봐도 동일하게 타임아웃이 나서, PaaS가
 * 아웃바운드 SMTP 포트 자체를 막아둔 것으로 판단했다. 일반 HTTPS 요청만 쓰는 이메일 API로 옮기면
 * 포트 차단과 무관해진다. spring-boot-starter-mail(JavaMailSender)은 더 이상 여기서 안 쓴다.
 *
 * 요청 바디는 Jackson 대신 직접 조립한다 - 이 프로젝트의 backend 빌드에는 com.fasterxml.jackson
 * .databind가 컴파일 클래스패스에 없어서(2026-08-27 배포 실패로 확인), 굳이 새 의존성을 추가하는
 * 대신 필드 4개짜리 단순 JSON을 문자열로 직접 만든다.
 *
 * 주의(운영자 확인 필요): Resend는 발신 도메인을 인증하기 전까지 "onboarding@resend.dev"로만
 * 보낼 수 있고, 그 상태에서는 Resend 계정 소유자 본인 이메일로만 발송이 허용되는 샌드박스
 * 제한이 있다. 실제 참가자 전원에게 인증 코드를 보내려면 Resend에서 소유한 도메인을 인증(DNS
 * 레코드 등록)해야 하고, 그 후 MAIL_FROM을 그 도메인 주소로 바꿔줘야 한다.
 */
@Service
public class MailService {

    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String apiKey;
    private final String fromAddress;

    public MailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${mail.from}") String fromAddress
    ) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(String toEmail, String nickname, String resetUrl) {
        String text =
                nickname + "님, 안녕하세요.\n\n"
                        + "아래 링크에서 새 비밀번호를 설정해주세요. 이 링크는 30분 동안만 유효합니다.\n\n"
                        + resetUrl
                        + "\n\n본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.";
        send(toEmail, "[프린세스 다이어리] 비밀번호 재설정 안내", text);
    }

    // 회원가입 전 이메일 인증 코드 (2026-08-26 요청).
    public void sendVerificationCodeEmail(String toEmail, String code) {
        String text =
                "아래 6자리 코드를 회원가입 화면에 입력해주세요. 이 코드는 10분 동안만 유효합니다.\n\n"
                        + code
                        + "\n\n본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.";
        send(toEmail, "[프린세스 다이어리] 이메일 인증 코드", text);
    }

    private void send(String toEmail, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "RESEND_API_KEY is not configured - set it in the environment before sending email");
        }
        try {
            String body = "{"
                    + "\"from\":\"" + jsonEscape(fromAddress) + "\","
                    + "\"to\":[\"" + jsonEscape(toEmail) + "\"],"
                    + "\"subject\":\"" + jsonEscape(subject) + "\","
                    + "\"text\":\"" + jsonEscape(text) + "\""
                    + "}";
            HttpRequest request = HttpRequest.newBuilder(RESEND_ENDPOINT)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Resend API request failed with status " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send email via Resend", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending email via Resend", e);
        }
    }

    // Jackson 없이 손으로 JSON 문자열을 조립하다 보니 이스케이프도 직접 해야 한다 - 이메일 본문에
    // 개행/따옴표/역슬래시가 들어가면 JSON이 깨지므로 최소한의 이스케이프만 처리한다.
    private String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
