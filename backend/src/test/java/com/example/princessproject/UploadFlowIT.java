package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.upload.dto.UploadResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void uploadedFileIsRetrievableAtReturnedUrl() throws Exception {
        LoginResponse login = client.post().uri("/api/auth/signup")
                .body(TestAccountSupport.verifiedSignup(emailVerificationRepository, "upload-tester"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
        String auth = "Bearer " + login.token();

        ByteArrayResource photo = new ByteArrayResource(jpegWithoutExif()) {
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

    /** 갤러리 사진도 허용하므로 EXIF 촬영일이 없는 일반 JPEG도 업로드되어야 한다. */
    private byte[] jpegWithoutExif() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
