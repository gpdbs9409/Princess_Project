package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.web.dto.LoginRequest;
import com.example.princessproject.web.dto.LoginResponse;
import com.example.princessproject.web.dto.UploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Confirms the upload -> local storage -> static serving round trip works with no AWS
 * credentials configured (LocalFileStorageClient is the active bean in this profile).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UploadFlowIT {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void uploadedFileIsRetrievableAtReturnedUrl() {
        LoginResponse login = client.post().uri("/api/auth/login")
                .body(new LoginRequest("upload-tester"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        String auth = "Bearer " + login.token();

        ByteArrayResource photo = new ByteArrayResource("fake-image-bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        };
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", photo);

        UploadResponse uploadResponse = client.post().uri("/api/uploads")
                .header("Authorization", auth)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UploadResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(uploadResponse.url()).startsWith("/uploads/").endsWith(".jpg");

        client.get().uri(uploadResponse.url())
                .exchange()
                .expectStatus().isOk();
    }
}
