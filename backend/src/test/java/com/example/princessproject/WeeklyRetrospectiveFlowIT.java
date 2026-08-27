package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.dto.CommonTaskResponse;
import com.example.princessproject.commontask.model.CommonTaskType;
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
    void eachSaveCreatesANewCardHistoryIsNewestFirstAndOneCardCanBeEdited() {
        LocalDate currentMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        save(currentMonday.minusWeeks(2), "가장 오래된 회고");
        save(currentMonday.minusWeeks(1), "지난주 회고");

        CommonTaskResponse firstCurrent = save(currentMonday.plusDays(3), "이번주 첫 내용");
        CommonTaskResponse secondCurrent = save(currentMonday.plusDays(6), "이번주 두 번째 내용");

        assertThat(secondCurrent.id()).isNotEqualTo(firstCurrent.id());
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

        assertThat(List.of(history).stream().map(CommonTaskResponse::id).toList())
                .containsExactly(secondCurrent.id(), firstCurrent.id(), history[2].id(), history[3].id());
        assertThat(List.of(history).stream().map(CommonTaskResponse::retroWeekReview).toList())
                .containsExactly("이번주 수정 내용", "이번주 첫 내용", "지난주 회고", "가장 오래된 회고");
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
