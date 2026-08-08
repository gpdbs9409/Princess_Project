package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.aifeedback.dto.AiFeedbackResponse;
import com.example.princessproject.auth.dto.LoginRequest;
import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.catalog.dto.GoalTypeResponse;
import com.example.princessproject.catalog.dto.MissionDefinitionResponse;
import com.example.princessproject.catalog.dto.StatTypeResponse;
import com.example.princessproject.catalog.model.MissionType;
import com.example.princessproject.common.model.GoalTypeCode;
import com.example.princessproject.project.dto.ProjectResponse;
import com.example.princessproject.project.dto.ProjectSelectionsRequest;
import com.example.princessproject.record.dto.DailySummaryResponse;
import com.example.princessproject.record.dto.RecordRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Exercises the full login -> catalog -> selections -> mission input -> AI feedback flow over
 * real HTTP against an H2-backed context, mirroring what the frontend does against MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DailyMissionFlowIT {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private LoginResponse login(String nickname) {
        return client.post().uri("/api/auth/signup")
                .body(new LoginRequest(nickname, "test-password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private List<GoalTypeResponse> getCatalog(String auth) {
        return List.of(client.get().uri("/api/catalog")
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GoalTypeResponse[].class)
                .returnResult()
                .getResponseBody());
    }

    @Test
    void fullDailyFlowComputesScoresAndGeneratesMockFeedback() {
        LoginResponse login = login("test-princess");
        String auth = "Bearer " + login.token();
        Long userId = login.user().id();
        assertThat(userId).isNotNull();

        List<GoalTypeResponse> catalog = getCatalog(auth);
        GoalTypeResponse physical = catalog.stream()
                .filter(g -> g.code() == GoalTypeCode.PHYSICAL).findFirst().orElseThrow();
        GoalTypeResponse psychology = catalog.stream()
                .filter(g -> g.code() == GoalTypeCode.PSYCHOLOGY).findFirst().orElseThrow();
        StatTypeResponse exerciseStat = physical.stats().get(0);
        MissionDefinitionResponse exerciseMission = exerciseStat.missions().get(0);
        StatTypeResponse psychologyStat = psychology.stats().get(0);
        MissionDefinitionResponse journalMission = psychologyStat.missions().get(0);

        ProjectSelectionsRequest selections = new ProjectSelectionsRequest(
                "건강한 사람", "단정한 사람", "매일 운동하는 사람",
                List.of(
                        new ProjectSelectionsRequest.GoalSelection(GoalTypeCode.PHYSICAL, 70, null, List.of(
                                new ProjectSelectionsRequest.StatSelection(exerciseStat.id(), 100, null, List.of(
                                        new ProjectSelectionsRequest.MissionSelection(
                                                exerciseMission.id(), null,
                                                exerciseMission.defaultTargetValue(), exerciseMission.unit(),
                                                exerciseMission.defaultAssignedPoints(), MissionType.DAILY)
                                ))
                        )),
                        new ProjectSelectionsRequest.GoalSelection(GoalTypeCode.PSYCHOLOGY, 30, null, List.of(
                                new ProjectSelectionsRequest.StatSelection(psychologyStat.id(), 100, null, List.of(
                                        new ProjectSelectionsRequest.MissionSelection(
                                                journalMission.id(), null,
                                                journalMission.defaultTargetValue(), journalMission.unit(),
                                                journalMission.defaultAssignedPoints(), MissionType.DAILY)
                                ))
                        ))
                )
        );

        ProjectResponse project = client.put().uri("/api/projects/active/selections")
                .header("Authorization", auth)
                .body(selections)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectResponse.class)
                .returnResult()
                .getResponseBody();

        Long exerciseUserMissionId = project.goals().get(0).stats().get(0).missions().get(0).id();
        Long journalUserMissionId = project.goals().get(1).stats().get(0).missions().get(0).id();
        LocalDate today = LocalDate.now();

        DailySummaryResponse afterExercise = client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(exerciseUserMissionId, today, exerciseMission.defaultTargetValue(), "https://example.com/photo.jpg", "완료!", null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(afterExercise.totalScore()).isEqualByComparingTo(exerciseMission.defaultAssignedPoints());
        assertThat(afterExercise.completedMissions()).contains(exerciseMission.name());

        DailySummaryResponse afterJournal = client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(journalUserMissionId, today, journalMission.defaultTargetValue(), "https://example.com/photo.jpg", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        BigDecimal expectedTotal = exerciseMission.defaultAssignedPoints().add(journalMission.defaultAssignedPoints());
        assertThat(afterJournal.totalScore()).isEqualByComparingTo(expectedTotal);
        assertThat(afterJournal.completedMissions())
                .containsExactlyInAnyOrder(exerciseMission.name(), journalMission.name());

        DailySummaryResponse summary = client.get().uri("/api/projects/active/daily?date={date}", today)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(summary.totalScore()).isEqualByComparingTo(expectedTotal);

        AiFeedbackResponse feedback = client.post().uri("/api/projects/active/ai-feedback?date={date}", today)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AiFeedbackResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(feedback.summary()).isNotBlank();
        assertThat(feedback.cheer()).isNotBlank();

        DailySummaryResponse summaryWithFeedback = client.get().uri("/api/projects/active/daily?date={date}", today)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(summaryWithFeedback.aiFeedback()).isNotNull();
        assertThat(summaryWithFeedback.aiFeedback().summary()).isEqualTo(feedback.summary());
    }

    @Test
    void tokenCannotBeUsedToRecordAgainstAnotherUsersMission() {
        LoginResponse userA = login("princess-a");
        LoginResponse userB = login("princess-b");
        String authA = "Bearer " + userA.token();
        String authB = "Bearer " + userB.token();

        List<GoalTypeResponse> catalog = getCatalog(authA);
        GoalTypeResponse physical = catalog.stream()
                .filter(g -> g.code() == GoalTypeCode.PHYSICAL).findFirst().orElseThrow();
        StatTypeResponse exerciseStat = physical.stats().get(0);
        MissionDefinitionResponse exerciseMission = exerciseStat.missions().get(0);

        ProjectSelectionsRequest selections = new ProjectSelectionsRequest(
                null, null, null,
                List.of(new ProjectSelectionsRequest.GoalSelection(GoalTypeCode.PHYSICAL, 100, null, List.of(
                        new ProjectSelectionsRequest.StatSelection(exerciseStat.id(), 100, null, List.of(
                                new ProjectSelectionsRequest.MissionSelection(
                                        exerciseMission.id(), null,
                                        exerciseMission.defaultTargetValue(), exerciseMission.unit(),
                                        exerciseMission.defaultAssignedPoints(), MissionType.DAILY)
                        ))
                )))
        );

        ProjectResponse projectA = client.put().uri("/api/projects/active/selections")
                .header("Authorization", authA)
                .body(selections)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectResponse.class)
                .returnResult()
                .getResponseBody();
        Long userAMissionId = projectA.goals().get(0).stats().get(0).missions().get(0).id();

        client.post().uri("/api/records")
                .header("Authorization", authB)
                .body(new RecordRequest(userAMissionId, LocalDate.now(), BigDecimal.TEN, "https://example.com/photo.jpg", null, null))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void missingTokenIsRejected() {
        client.get().uri("/api/catalog")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
