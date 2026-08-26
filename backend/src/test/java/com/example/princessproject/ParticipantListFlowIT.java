package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.project.model.ProjectStatus;
import com.example.princessproject.project.model.UserProject;
import com.example.princessproject.project.repository.UserProjectRepository;
import com.example.princessproject.user.dto.ParticipantResponse;
import com.example.princessproject.user.model.User;
import com.example.princessproject.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ParticipantListFlowIT {

    @LocalServerPort
    private int port;

    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProjectRepository userProjectRepository;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void returnsTheLatestThreeOnboardingProfileValuesForCohortParticipants() {
        LoginResponse requesterLogin = client.post().uri("/api/auth/signup")
                .body(TestAccountSupport.verifiedSignup(emailVerificationRepository, "participant-requester"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        User requester = userRepository.findById(requesterLogin.user().id()).orElseThrow();
        requester.setCohort("QA-기수");
        userRepository.save(requester);

        User companion = new User("함께하는공주", "unused-test-hash");
        companion.setCohort("QA-기수");
        companion = userRepository.save(companion);

        UserProject older = new UserProject(companion, "이전 프로젝트");
        older.setStatus(ProjectStatus.ACTIVE);
        older.setGoalAppearance("이전 추구미");
        userProjectRepository.saveAndFlush(older);

        UserProject latest = new UserProject(companion, "최신 프로젝트");
        latest.setStatus(ProjectStatus.COMPLETED);
        latest.setGoalAppearance("우아하고 단정한 분위기");
        latest.setGoalHuman("꾸준히 성장하는 사람");
        latest.setGoalEnding("매일 작은 약속을 지키는 사람");
        userProjectRepository.saveAndFlush(latest);

        ParticipantResponse[] participants = client.get()
                .uri("/api/users/{id}/participants", requester.getId())
                .header("Authorization", "Bearer " + requesterLogin.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ParticipantResponse[].class)
                .returnResult()
                .getResponseBody();

        assertThat(participants).anySatisfy(participant -> {
            assertThat(participant.nickname()).isEqualTo("함께하는공주");
            assertThat(participant.goalAppearance()).isEqualTo("우아하고 단정한 분위기");
            assertThat(participant.goalHuman()).isEqualTo("꾸준히 성장하는 사람");
            assertThat(participant.goalEnding()).isEqualTo("매일 작은 약속을 지키는 사람");
        });
    }
}
