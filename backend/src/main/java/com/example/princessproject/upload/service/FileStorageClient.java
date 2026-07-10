package com.example.princessproject.upload.service;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageClient {

    /**
     * Stores the file and returns a URL the client can use to display it later.
     */
    String store(MultipartFile file);

    /**
     * Loads back a previously stored file by the key embedded in its URL. Only needed by
     * backends whose storage isn't already reachable via a public static URL (e.g. S3-compatible
     * buckets that are private by default, such as Railway Buckets) - LocalFileStorageClient never
     * needs this since /uploads/** already serves its files directly.
     */
    default StoredFile load(String key) {
        throw new UnsupportedOperationException("load() is not supported by " + getClass().getSimpleName());
    }

    record StoredFile(byte[] content, String contentType) {
    }

    static String generateFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                extension = originalFilename.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");
            }
        }
        return UUID.randomUUID() + extension;
    }
}
