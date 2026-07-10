package com.example.princessproject.service.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageClient {

    /**
     * Stores the file and returns a URL the client can use to display it later.
     */
    String store(MultipartFile file);

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
