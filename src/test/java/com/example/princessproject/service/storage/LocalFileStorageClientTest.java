package com.example.princessproject.service.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageClientTest {

    private final LocalFileStorageClient client = new LocalFileStorageClient();

    @Test
    void storesFileAndReturnsUploadsUrlPreservingExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        String url = client.store(file);

        assertThat(url).startsWith("/uploads/").endsWith(".jpg");
    }

    @Test
    void handlesMissingExtensionGracefully() {
        MockMultipartFile file = new MockMultipartFile("file", "noext", "application/octet-stream", "data".getBytes());

        String url = client.store(file);

        assertThat(url).startsWith("/uploads/");
    }
}
