package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.aifeedback.dto.AiFeedbackResponse;
import com.example.princessproject.admin.dto.AdminMemberWeekResponse;
import com.example.princessproject.admin.service.AdminService;
import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.catalog.dto.GoalTypeResponse;
import com.example.princessproject.catalog.dto.MissionDefinitionResponse;
import com.example.princessproject.catalog.dto.StatTypeResponse;
import com.example.princessproject.catalog.model.MissionType;
import com.example.princessproject.common.model.GoalTypeCode;
import com.example.princessproject.common.ApiErrorResponse;
import com.example.princessproject.commontask.dto.CommonTaskRequest;
import com.example.princessproject.commontask.model.CommonTaskType;
import com.example.princessproject.project.dto.ProjectResponse;
import com.example.princessproject.project.dto.ProjectSelectionsRequest;
import com.example.princessproject.record.dto.DailySummaryResponse;
import com.example.princessproject.record.dto.RecordRequest;
import com.example.princessproject.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private LoginResponse login(String nickname) {
        return client.post().uri("/api/auth/signup")
                .body(TestAccountSupport.verifiedSignup(emailVerificationRepository, nickname))
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
        // 배점은 자본 비중에서 나온다: 개인 미션 80점 중 PHYSICAL 70% -> 56점, 미션 1개라 전액.
        assertThat(afterExercise.totalScore()).isEqualByComparingTo(new BigDecimal("56.00"));
        assertThat(afterExercise.maxPossible()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(afterExercise.completedMissions()).contains(exerciseMission.name());

        DailySummaryResponse afterJournal = client.post().uri("/api/records")
                .header("Authorization", auth)
                .body(new RecordRequest(journalUserMissionId, today, journalMission.defaultTargetValue(), "https://example.com/photo.jpg", null, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailySummaryResponse.class)
                .returnResult()
                .getResponseBody();
        // PSYCHOLOGY 30% -> 80 x 0.3 = 24점. 둘 다 100% 달성하면 56 + 24 = 80점.
        BigDecimal expectedTotal = new BigDecimal("80.00");
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

        // 환급 출석은 DAILY 개인 미션뿐 아니라 그날의 독서·공부까지 모두 본다.
        // 개인 미션을 전부 끝냈어도 공통과제 둘이 남았으므로 이 날은 부분 성공(0.5일)이다.
        var user = userRepository.findById(userId).orElseThrow();
        user.setCohort("1기");
        userRepository.saveAndFlush(user);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int todayIndex = today.getDayOfWeek().getValue() - 1;
        AdminMemberWeekResponse beforeCommonTasks = adminService.listParticipantsForWeek("1기", weekStart).stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst().orElseThrow();
        assertThat(beforeCommonTasks.dailyCredits().get(todayIndex)).isEqualTo(0.5);

        saveCommonTask(auth, new CommonTaskRequest(
                CommonTaskType.READING, today, "테스트 책", 1, 2,
                null, null, null, null, null,
                "https://example.com/reading.jpg", true, null));
        AdminMemberWeekResponse afterReading = adminService.listParticipantsForWeek("1기", weekStart).stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst().orElseThrow();
        assertThat(afterReading.dailyCredits().get(todayIndex)).isEqualTo(0.5);

        saveCommonTask(auth, new CommonTaskRequest(
                CommonTaskType.STUDY, today, null, null, null,
                BigDecimal.TEN, BigDecimal.TEN, null, null, null,
                "https://example.com/study.jpg", true, null));
        AdminMemberWeekResponse afterReadingAndStudy = adminService.listParticipantsForWeek("1기", weekStart).stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst().orElseThrow();
        assertThat(afterReadingAndStudy.dailyCredits().get(todayIndex)).isEqualTo(1.0);

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

    private void saveCommonTask(String auth, CommonTaskRequest request) {
        client.post().uri("/api/common-tasks")
                .header("Authorization", auth)
                .body(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void weeklyMissionCannotBeCreatedThroughTheApi() {
        LoginResponse login = login("weekly-mission-rejected");
        String auth = "Bearer " + login.token();
        GoalTypeResponse physical = getCatalog(auth).stream()
                .filter(goal -> goal.code() == GoalTypeCode.PHYSICAL)
                .findFirst().orElseThrow();
        StatTypeResponse stat = physical.stats().get(0);
        MissionDefinitionResponse mission = stat.missions().get(0);

        ProjectSelectionsRequest selections = new ProjectSelectionsRequest(
                null, null, null,
                List.of(new ProjectSelectionsRequest.GoalSelection(GoalTypeCode.PHYSICAL, 100, null, List.of(
                        new ProjectSelectionsRequest.StatSelection(stat.id(), 100, null, List.of(
                                new ProjectSelectionsRequest.MissionSelection(
                                        mission.id(), null, mission.defaultTargetValue(), mission.unit(),
                                        mission.defaultAssignedPoints(), MissionType.WEEKLY)
                        ))
                )))
        );

        ApiErrorResponse error = client.put().uri("/api/projects/active/selections")
                .header("Authorization", auth)
                .body(selections)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(error.code()).isEqualTo("MISSION_TYPE_UNSUPPORTED");
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
