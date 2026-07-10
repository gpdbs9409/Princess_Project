package com.example.princessproject.web;

import com.example.princessproject.service.storage.FileStorageClient;
import com.example.princessproject.web.dto.UploadResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {

    private final FileStorageClient fileStorageClient;

    public UploadController(FileStorageClient fileStorageClient) {
        this.fileStorageClient = fileStorageClient;
    }

    @PostMapping("/api/uploads")
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        return new UploadResponse(fileStorageClient.store(file));
    }
}
