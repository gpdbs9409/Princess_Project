package com.example.princessproject.upload.service;

import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Activates once storage.bucket.name is set. Talks to any S3-API-compatible object storage
 * (Railway Buckets, MinIO, Cloudflare R2, ...) - the S3 Java SDK is just the client library
 * for that protocol, this isn't tied to AWS itself. Credentials are read from our own
 * bucket.* properties and passed explicitly, rather than relying on the SDK's default
 * AWS_-prefixed env var chain (this app was never going to be pointed at real AWS S3).
 *
 * <p>Railway Buckets (and most non-AWS S3-compatible buckets) are private by default with no
 * public object URL, so {@link #store} returns a URL that is proxied through our own
 * {@code GET /api/uploads/{key}} endpoint (see UploadController) instead of a direct bucket
 * URL - that way the link stays valid regardless of the bucket's public-access settings.
 */
@Component
@ConditionalOnExpression("!'${storage.bucket.name:}'.isEmpty()")
public class BucketFileStorageClient implements FileStorageClient {

    private final S3Client s3Client;
    private final String bucket;

    public BucketFileStorageClient(
            @Value("${storage.bucket.name}") String bucket,
            @Value("${storage.bucket.region}") String region,
            @Value("${storage.bucket.endpoint:}") String endpoint,
            @Value("${storage.bucket.path-style-access:true}") boolean pathStyleAccess,
            @Value("${storage.bucket.access-key-id:}") String accessKeyId,
            @Value("${storage.bucket.secret-access-key:}") String secretAccessKey
    ) {
        this.bucket = bucket;
        var builder = S3Client.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            // Path-style (endpoint/bucket/key) vs virtual-host style (bucket.endpoint/key)
            // varies by provider - e.g. MinIO defaults to path-style, but Railway Buckets
            // (Tigris-backed) use virtual-host style, so this must be configurable.
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(pathStyleAccess);
        }
        if (!accessKeyId.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
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
            throw new IllegalStateException("Failed to upload file to bucket", e);
        }
        return "/api/uploads/" + key;
    }

    @Override
    public StoredFile load(String key) {
        try (ResponseInputStream<GetObjectResponse> object = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return new StoredFile(object.readAllBytes(), object.response().contentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load file from bucket: " + key, e);
        }
    }
}
