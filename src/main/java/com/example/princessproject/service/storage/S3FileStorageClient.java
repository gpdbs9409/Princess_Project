package com.example.princessproject.service.storage;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Activates once aws.s3.bucket is set. Credentials come from the default AWS provider chain
 * (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY env vars, instance profile, etc) - never hardcoded.
 */
@Component
@ConditionalOnExpression("!'${aws.s3.bucket:}'.isEmpty()")
public class S3FileStorageClient implements FileStorageClient {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3FileStorageClient(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region
    ) {
        this.bucket = bucket;
        this.region = region;
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public String store(MultipartFile file) {
        String key = FileStorageClient.generateFilename(file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload file to S3", e);
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }
}
