package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.retrospective.dto.WeeklyRetrospectiveRequest;
import com.example.princessproject.retrospective.dto.WeeklyRetrospectiveResponse;
import com.example.princessproject.record.dto.WeeklyReportResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WeeklyRetrospectiveFlowIT {

    @LocalServerPort
    private int port;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    private RestTestClient client;
    private String auth;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        LoginResponse login = client.post().uri("/api/auth/signup")
                .body(TestAccountSupport.verifiedSignup(emailVerificationRepository, "retro-tester"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        auth = "Bearer " + login.token();
    }

    @Test
    void oneRetrospectivePerWeekHistoryExcludesThisWeekAndTheCardCanBeEdited() {
        LocalDate currentMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        save(currentMonday.minusWeeks(2), "가장 오래된 회고");
        save(currentMonday.minusWeeks(1), "지난주 회고");

        WeeklyRetrospectiveResponse firstCurrent = save(currentMonday.plusDays(3), "이번주 첫 내용");
        // 주 1회 원칙: 같은 주에 POST로 새 회고를 만들 수 없고 기존 카드의 수정만 허용한다.
        client.post().uri("/api/weekly-retrospectives")
                .header("Authorization", auth)
                .body(request(currentMonday.plusDays(6), "이번주 두 번째 내용"))
                .exchange()
                .expectStatus().isBadRequest();

        WeeklyRetrospectiveResponse updated = client.put()
                .uri("/api/weekly-retrospectives/{id}", firstCurrent.id())
                .header("Authorization", auth)
                .body(request(currentMonday, "이번주 수정 내용"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyRetrospectiveResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(updated.id()).isEqualTo(firstCurrent.id());
        assertThat(updated.retroWeekReview()).isEqualTo("이번주 수정 내용");

        WeeklyRetrospectiveResponse current = client.get()
                .uri("/api/weekly-retrospectives?weekStart={weekStart}", currentMonday)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyRetrospectiveResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(current.id()).isEqualTo(firstCurrent.id());

        WeeklyRetrospectiveResponse[] history = client.get()
                .uri("/api/weekly-retrospectives/history?weekStart={weekStart}", currentMonday)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyRetrospectiveResponse[].class)
                .returnResult()
                .getResponseBody();

        // 이번 주 회고는 위쪽 작성 폼이 이미 보여주므로 지난 회고 목록에서는 빠진다.
        assertThat(List.of(history).stream().map(WeeklyRetrospectiveResponse::retroWeekReview).toList())
                .containsExactly("지난주 회고", "가장 오래된 회고");
        assertThat(List.of(history).stream().map(WeeklyRetrospectiveResponse::id).toList())
                .doesNotContain(firstCurrent.id());

        // 주간 회고는 선택 과제다. 여러 번 작성·수정해도 주간 점수에는 들어가지 않는다.
        WeeklyReportResponse report = client.get()
                .uri("/api/projects/active/weekly-report?weekStart={weekStart}", currentMonday)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyReportResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(report.totalScore()).isEqualByComparingTo("0");
        assertThat(report.missionCompletionCounts()).doesNotContainKey("주간 회고");
    }

    private WeeklyRetrospectiveResponse save(LocalDate date, String weekReview) {
        WeeklyRetrospectiveRequest request = request(date, weekReview);
        return client.post().uri("/api/weekly-retrospectives")
                .header("Authorization", auth)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyRetrospectiveResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private WeeklyRetrospectiveRequest request(LocalDate date, String weekReview) {
        return new WeeklyRetrospectiveRequest(date, null, weekReview, null);
    }
}
