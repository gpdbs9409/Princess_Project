package com.example.princessproject.upload.controller;

import com.example.princessproject.upload.service.FileStorageClient;
import com.example.princessproject.upload.dto.UploadResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * Proxies files back out of storage backends whose buckets are private by default
     * (e.g. Railway Buckets) - see S3FileStorageClient. Public (see SecurityConfig): an
     * <img src> can't attach an Authorization header.
     */
    @GetMapping("/api/uploads/{key}")
    public ResponseEntity<byte[]> download(@PathVariable String key) {
        FileStorageClient.StoredFile file = fileStorageClient.load(key);
        MediaType contentType = file.contentType() != null
                ? MediaType.parseMediaType(file.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(file.content());
    }
}
