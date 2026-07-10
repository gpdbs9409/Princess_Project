package com.example.princessproject.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Active while no S3 bucket is configured. Stores files under ./uploads, served back via the
 * /uploads/** resource handler in WebMvcConfig - lets the upload flow work with no AWS
 * credentials, mirroring how MockAiFeedbackClient stands in before an OpenAI key is set.
 */
@Component
@ConditionalOnExpression("'${aws.s3.bucket:}'.isEmpty()")
public class LocalFileStorageClient implements FileStorageClient {

    private static final Path UPLOAD_DIR = Path.of("uploads");

    @Override
    public String store(MultipartFile file) {
        try {
            Files.createDirectories(UPLOAD_DIR);
            String filename = FileStorageClient.generateFilename(file.getOriginalFilename());
            file.transferTo(UPLOAD_DIR.resolve(filename));
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file locally", e);
        }
    }
}
