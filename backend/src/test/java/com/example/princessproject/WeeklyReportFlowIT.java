package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginRequest;
import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.catalog.dto.GoalTypeResponse;
import com.example.princessproject.catalog.dto.MissionDefinitionResponse;
import com.example.princessproject.catalog.dto.StatTypeResponse;
import com.example.princessproject.catalog.model.MissionType;
import com.example.princessproject.common.model.GoalTypeCode;
import com.example.princessproject.project.dto.ProjectResponse;
import com.example.princessproject.project.dto.ProjectSelectionsRequest;
import com.example.princessproject.record.dto.RecordRequest;
import com.example.princessproject.record.dto.WeeklyReportResponse;
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
        LoginResponse login = client.post().uri("/api/auth/signup")
                .body(new LoginRequest("weekly-tester", "test-password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        String auth = "Bearer " + login.token();

        GoalTypeResponse[] catalog = client.get().uri("/api/catalog")
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GoalTypeResponse[].class)
                .returnResult()
                .getResponseBody();
        GoalTypeResponse physical = List.of(catalog).stream()
                .filter(g -> g.code() == GoalTypeCode.PHYSICAL).findFirst().orElseThrow();
        StatTypeResponse exerciseStat = physical.stats().get(0);
        MissionDefinitionResponse exerciseMission = exerciseStat.missions().get(0);

        ProjectSelectionsRequest selections = new ProjectSelectionsRequest(
                null, null,
                List.of(new ProjectSelectionsRequest.GoalSelection(GoalTypeCode.PHYSICAL, 100, null, List.of(
                        new ProjectSelectionsRequest.StatSelection(exerciseStat.id(), 100, null, List.of(
                                new ProjectSelectionsRequest.MissionSelection(
                                        exerciseMission.id(), null,
                                        exerciseMission.defaultTargetValue(), exerciseMission.unit(),
                                        exerciseMission.defaultAssignedPoints(), MissionType.DAILY)
                        ))
                )))
        );
        ProjectResponse project = client.put().uri("/api/projects/active/selections")
                .header("Authorization", auth)
                .body(selections)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectResponse.class)
                .returnResult()
                .getResponseBody();
        Long userMissionId = project.goals().get(0).stats().get(0).missions().get(0).id();

        // Anchor both records to fixed offsets from the week's Monday (rather than
        // "today"/"yesterday") so the test doesn't break when run on a Sunday/Monday boundary.
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate day1 = weekStart;
        LocalDate day2 = weekStart.plusDays(1);

        client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(userMissionId, day1, exerciseMission.defaultTargetValue(), "https://example.com/photo1.jpg", null, null))
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(userMissionId, day2, exerciseMission.defaultTargetValue(), "https://example.com/photo2.jpg", null, null))
                .exchange()
                .expectStatus().isOk();

        WeeklyReportResponse report = client.get()
                .uri("/api/projects/active/weekly-report?weekStart={weekStart}", weekStart)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyReportResponse.class)
                .returnResult()
                .getResponseBody();

        java.math.BigDecimal expectedTotal = exerciseMission.defaultAssignedPoints()
                .add(exerciseMission.defaultAssignedPoints());
        assertThat(report.weekStart()).isEqualTo(weekStart);
        assertThat(report.totalScore()).isEqualByComparingTo(expectedTotal);
        assertThat(report.statScoreTotals().get("physical")).isEqualByComparingTo(expectedTotal);
        assertThat(report.missionCompletionCounts()).containsEntry(exerciseMission.name(), 2);
        assertThat(report.dailyBreakdown()).hasSize(7);
    }
}
