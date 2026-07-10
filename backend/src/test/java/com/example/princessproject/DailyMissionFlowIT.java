package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.common.model.StatType;
import com.example.princessproject.aifeedback.dto.AiFeedbackResponse;
import com.example.princessproject.record.dto.DailySummaryResponse;
import com.example.princessproject.auth.dto.LoginRequest;
import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.mission.dto.MissionResponse;
import com.example.princessproject.record.dto.RecordRequest;
import com.example.princessproject.user.dto.StatFocusRequest;
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
 * Exercises the full login -> stat focus -> mission input -> AI feedback flow over real HTTP
 * against an H2-backed context, mirroring what the static frontend does against MySQL.
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
        return client.post().uri("/api/auth/login")
                .body(new LoginRequest(nickname))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void fullDailyFlowComputesScoresAndGeneratesMockFeedback() {
        LoginResponse login = login("test-princess");
        String auth = "Bearer " + login.token();
        Long userId = login.user().id();
        assertThat(userId).isNotNull();

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
        assertThat(missions).hasSize(5);

        MissionResponse exercise = List.of(missions).stream().filter(m -> m.name().equals("운동")).findFirst().orElseThrow();
        MissionResponse journal = List.of(missions).stream().filter(m -> m.name().equals("일기")).findFirst().orElseThrow();
        LocalDate today = LocalDate.now();

        DailySummaryResponse afterExercise = client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(userId, exercise.id(), today, 1.0, null, "완료!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(afterExercise.totalScore()).isEqualTo(20.0);
        assertThat(afterExercise.completedMissions()).contains("운동");

        DailySummaryResponse afterJournal = client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(userId, journal.id(), today, 1.0, null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(afterJournal.totalScore()).isEqualTo(35.0);
        assertThat(afterJournal.completedMissions()).containsExactlyInAnyOrder("운동", "일기");
        assertThat(afterJournal.remainingMissions()).containsExactlyInAnyOrder("식단", "마스크팩", "6시 기상");

        DailySummaryResponse summary = client.get().uri("/api/users/{id}/daily?date={date}", userId, today)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(summary.totalScore()).isEqualTo(35.0);

        AiFeedbackResponse feedback = client.post().uri("/api/users/{id}/ai-feedback?date={date}", userId, today)
                .header("Authorization", auth)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AiFeedbackResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(feedback.summary()).isNotBlank();
        assertThat(feedback.cheer()).isNotBlank();

        DailySummaryResponse summaryWithFeedback = client.get().uri("/api/users/{id}/daily?date={date}", userId, today)
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
    void tokenCannotBeUsedToReadAnotherUsersData() {
        LoginResponse userA = login("princess-a");
        LoginResponse userB = login("princess-b");

        client.get().uri("/api/users/{id}/daily?date={date}", userB.user().id(), LocalDate.now())
                .header("Authorization", "Bearer " + userA.token())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingTokenIsRejected() {
        client.get().uri("/api/missions")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
