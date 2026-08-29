package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.dto.CommonTaskResponse;
import com.example.princessproject.commontask.model.CommonTaskType;
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

        CommonTaskResponse firstCurrent = save(currentMonday.plusDays(3), "이번주 첫 내용");
        CommonTaskResponse secondCurrent = save(currentMonday.plusDays(6), "이번주 두 번째 내용");

        // 주 1회 원칙: 같은 주에 다시 저장하면 새 카드가 쌓이지 않고 그 주 카드가 갱신된다.
        assertThat(secondCurrent.id()).isEqualTo(firstCurrent.id());
        assertThat(secondCurrent.retroWeekReview()).isEqualTo("이번주 두 번째 내용");
        assertThat(secondCurrent.recordDate()).isEqualTo(currentMonday);

        CommonTaskResponse updated = client.put()
                .uri("/api/common-tasks/weekly/{id}", secondCurrent.id())
                .header("Authorization", auth)
                .body(request(currentMonday, "이번주 수정 내용"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(CommonTaskResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(updated.id()).isEqualTo(secondCurrent.id());
        assertThat(updated.retroWeekReview()).isEqualTo("이번주 수정 내용");

        CommonTaskResponse current = client.get()
                .uri("/api/common-tasks/weekly?weekStart={weekStart}", currentMonday)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CommonTaskResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(current.id()).isEqualTo(secondCurrent.id());

        CommonTaskResponse[] history = client.get()
                .uri("/api/common-tasks/weekly/history?weekStart={weekStart}", currentMonday)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CommonTaskResponse[].class)
                .returnResult()
                .getResponseBody();

        // 이번 주 회고는 위쪽 작성 폼이 이미 보여주므로 지난 회고 목록에서는 빠진다.
        assertThat(List.of(history).stream().map(CommonTaskResponse::retroWeekReview).toList())
                .containsExactly("지난주 회고", "가장 오래된 회고");
        assertThat(List.of(history).stream().map(CommonTaskResponse::id).toList())
                .doesNotContain(secondCurrent.id());

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

    private CommonTaskResponse save(LocalDate date, String weekReview) {
        CommonTaskRequest request = request(date, weekReview);
        return client.post().uri("/api/common-tasks")
                .header("Authorization", auth)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CommonTaskResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private CommonTaskRequest request(LocalDate date, String weekReview) {
        return new CommonTaskRequest(
                CommonTaskType.WEEKLY_RETROSPECTIVE,
                date,
                null,
                null,
                null,
                null,
                null,
                null,
                weekReview,
                null,
                null,
                null,
                null);
    }
}
