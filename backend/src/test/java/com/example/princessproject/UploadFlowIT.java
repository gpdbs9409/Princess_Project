package com.example.princessproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.repository.EmailVerificationRepository;
import com.example.princessproject.upload.dto.UploadResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
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

        ByteArrayResource photo = new ByteArrayResource(jpegWithTodayExifDate()) {
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

    /**
     * PhotoDateVerifier rejects any upload with no readable EXIF capture date, so a plain
     * byte blob no longer exercises this flow - build a real JPEG with DateTimeOriginal set
     * to right now instead.
     */
    private byte[] jpegWithTodayExifDate() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baseOut = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baseOut);

        TiffOutputSet outputSet = new TiffOutputSet();
        TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dateStr);

        ByteArrayOutputStream exifOut = new ByteArrayOutputStream();
        new ExifRewriter().updateExifMetadataLossless(baseOut.toByteArray(), exifOut, outputSet);
        return exifOut.toByteArray();
    }
}
