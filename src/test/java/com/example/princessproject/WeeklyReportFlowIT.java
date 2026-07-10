package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.domain.StatType;
import com.example.princessproject.web.dto.LoginRequest;
import com.example.princessproject.web.dto.LoginResponse;
import com.example.princessproject.web.dto.MissionResponse;
import com.example.princessproject.web.dto.RecordRequest;
import com.example.princessproject.web.dto.StatFocusRequest;
import com.example.princessproject.web.dto.WeeklyReportResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Confirms the weekly-report date-range repository queries are wired correctly - the pure
 * aggregation math itself is covered by WeeklyReportServiceTest with no DB involved.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WeeklyReportFlowIT {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void aggregatesTwoDaysOfRecordsIntoTheContainingWeek() {
        LoginResponse login = client.post().uri("/api/auth/login")
                .body(new LoginRequest("weekly-tester"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        String auth = "Bearer " + login.token();
        Long userId = login.user().id();

        client.put().uri("/api/users/{id}/stat-focus", userId)
                .header("Authorization", auth)
                .body(new StatFocusRequest(List.of(new StatFocusRequest.StatFocusItem(StatType.PHYSICAL, 100))))
                .exchange()
                .expectStatus().isOk();

        MissionResponse[] missions = client.get().uri("/api/missions")
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MissionResponse[].class)
                .returnResult()
                .getResponseBody();
        MissionResponse exercise = List.of(missions).stream().filter(m -> m.name().equals("운동")).findFirst().orElseThrow();

        // Anchor both records to fixed offsets from the week's Monday (rather than
        // "today"/"yesterday") so the test doesn't break when run on a Sunday/Monday boundary.
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate day1 = weekStart;
        LocalDate day2 = weekStart.plusDays(1);

        client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(userId, exercise.id(), day1, 1.0, null, null))
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(userId, exercise.id(), day2, 1.0, null, null))
                .exchange()
                .expectStatus().isOk();

        WeeklyReportResponse report = client.get()
                .uri("/api/users/{id}/weekly-report?weekStart={weekStart}", userId, weekStart)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyReportResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(report.weekStart()).isEqualTo(weekStart);
        assertThat(report.totalScore()).isEqualTo(40.0);
        assertThat(report.statScoreTotals()).containsEntry("physical", 40.0);
        assertThat(report.missionCompletionCounts()).containsEntry("운동", 2);
        assertThat(report.dailyBreakdown()).hasSize(7);
    }
}
