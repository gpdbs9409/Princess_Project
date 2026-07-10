package com.example.princessproject.upload.service;

import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Activates once aws.s3.bucket is set. Credentials come from the default AWS provider chain
 * (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY env vars, instance profile, etc) - never hardcoded.
 *
 * <p>Supports any S3-compatible provider (real AWS S3, Railway Buckets, MinIO, R2, ...) via an
 * optional custom endpoint. Railway Buckets (and most non-AWS S3-compatible buckets) are private
 * by default with no public object URL, so {@link #store} returns a URL that is proxied through
 * our own {@code GET /api/uploads/{key}} endpoint (see UploadController) instead of a direct
 * bucket URL - that way the link stays valid regardless of the bucket's public-access settings.
 */
@Component
@ConditionalOnExpression("!'${aws.s3.bucket:}'.isEmpty()")
public class S3FileStorageClient implements FileStorageClient {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorageClient(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.endpoint:}") String endpoint
    ) {
        this.bucket = bucket;
        var builder = S3Client.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            // Non-AWS S3-compatible providers (Railway Buckets, MinIO, R2, ...) need an explicit
            // endpoint and virtual-hosted-style addressing disabled.
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        this.s3Client = builder.build();
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
        return "/api/uploads/" + key;
    }

    @Override
    public StoredFile load(String key) {
        try (ResponseInputStream<GetObjectResponse> object = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return new StoredFile(object.readAllBytes(), object.response().contentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load file from S3: " + key, e);
        }
    }
}
