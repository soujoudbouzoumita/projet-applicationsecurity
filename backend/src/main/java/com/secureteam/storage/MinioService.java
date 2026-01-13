package com.secureteam.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.io.InputStream;
import java.util.Optional;

/**
 * MinIO Storage Service
 * Handles secure file upload/download with externalized credential management
 * 
 * ✅ Security Features:
 * - Credentials loaded from environment variables (not hardcoded)
 * - No default values for sensitive data
 * - Proper error handling and logging (no credential leaks)
 * - Fail-fast approach if credentials missing
 */
@ApplicationScoped
public class MinioService {

    private static final Logger LOG = Logger.getLogger(MinioService.class);

    @Inject
    @ConfigProperty(name = "minio.url", defaultValue = "http://localhost:9000")
    private String minioUrl;

    @Inject
    @ConfigProperty(name = "minio.access-key")
    private Optional<String> accessKey;

    @Inject
    @ConfigProperty(name = "minio.secret-key")
    private Optional<String> secretKey;

    // Mocking MinIO client to avoid massive dependency bloat
    // In real implementation: import io.minio.MinioClient;
    // private MinioClient minioClient;

    /**
     * Initialize MinIO client with validated credentials
     * Fails fast if required credentials are missing
     */
    @PostConstruct
    public void init() {
        // Validate that required credentials are provided
        if (accessKey.isEmpty()) {
            throw new IllegalStateException(
                "❌ CRITICAL: MinIO access key not configured. " +
                "Set environment variable: SECRET_MINIO_ACCESS_KEY");
        }

        if (secretKey.isEmpty()) {
            throw new IllegalStateException(
                "❌ CRITICAL: MinIO secret key not configured. " +
                "Set environment variable: SECRET_MINIO_SECRET_KEY");
        }

        LOG.infov("[Storage] MinIO client initialized for URL: {0}", minioUrl);

        // In production:
        // MinioClient.Builder builder = MinioClient.builder();
        // builder.endpoint(minioUrl);
        // builder.credentials(accessKey.get(), secretKey.get());
        // this.minioClient = builder.build();
    }

    /**
     * Upload file to MinIO bucket
     * 
     * @param bucket    Target bucket name
     * @param objectName Target object key
     * @param stream    Input file stream
     * @param contentType MIME type
     */
    public void uploadFile(String bucket, String objectName, InputStream stream, String contentType) {
        try {
            // Input validation
            if (bucket == null || bucket.isEmpty()) {
                throw new IllegalArgumentException("Bucket name cannot be empty");
            }
            if (objectName == null || objectName.isEmpty()) {
                throw new IllegalArgumentException("Object name cannot be empty");
            }
            if (stream == null) {
                throw new IllegalArgumentException("Input stream cannot be null");
            }

            // Security: Sanitize bucket and object names (prevent path traversal)
            if (bucket.contains("..") || bucket.contains("/")) {
                throw new IllegalArgumentException("Invalid bucket name");
            }
            if (objectName.contains("../")) {
                throw new IllegalArgumentException("Invalid object name (path traversal detected)");
            }

            LOG.infov("[Storage] Upload initiated - Bucket: {0}, Object: {1}, ContentType: {2}",
                     bucket, objectName, contentType);

            // In production:
            // minioClient.putObject(
            //     PutObjectArgs.builder()
            //         .bucket(bucket)
            //         .object(objectName)
            //         .stream(stream, stream.available(), -1)
            //         .contentType(contentType)
            //         .build()
            // );

            LOG.infov("[Storage] Upload successful - Bucket: {0}, Object: {1}", bucket, objectName);

        } catch (IllegalArgumentException e) {
            LOG.warnv("[Storage] Invalid upload request: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.errorv(e, "[Storage] Upload failed for {0}/{1}: {2}", 
                      bucket, objectName, e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * Download file from MinIO bucket
     * 
     * @param bucket    Source bucket name
     * @param objectName Source object key
     * @return Input stream of file content
     */
    public InputStream downloadFile(String bucket, String objectName) {
        try {
            // Input validation
            if (bucket == null || bucket.isEmpty()) {
                throw new IllegalArgumentException("Bucket name cannot be empty");
            }
            if (objectName == null || objectName.isEmpty()) {
                throw new IllegalArgumentException("Object name cannot be empty");
            }

            // Security: Sanitize bucket and object names
            if (bucket.contains("..") || bucket.contains("/")) {
                throw new IllegalArgumentException("Invalid bucket name");
            }
            if (objectName.contains("../")) {
                throw new IllegalArgumentException("Invalid object name (path traversal detected)");
            }

            LOG.infov("[Storage] Download initiated - Bucket: {0}, Object: {1}", bucket, objectName);

            // In production:
            // InputStream stream = minioClient.getObject(
            //     GetObjectArgs.builder()
            //         .bucket(bucket)
            //         .object(objectName)
            //         .build()
            // );
            // return stream;

            // Mock return for now
            return InputStream.nullInputStream();

        } catch (IllegalArgumentException e) {
            LOG.warnv("[Storage] Invalid download request: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.errorv(e, "[Storage] Download failed for {0}/{1}: {2}", 
                      bucket, objectName, e.getMessage());
            throw new RuntimeException("File download failed", e);
        }
    }

    /**
     * Delete file from MinIO bucket
     * 
     * @param bucket    Target bucket name
     * @param objectName Target object key
     */
    public void deleteFile(String bucket, String objectName) {
        try {
            // Input validation
            if (bucket == null || bucket.isEmpty()) {
                throw new IllegalArgumentException("Bucket name cannot be empty");
            }
            if (objectName == null || objectName.isEmpty()) {
                throw new IllegalArgumentException("Object name cannot be empty");
            }

            // Security: Sanitize names
            if (bucket.contains("..") || bucket.contains("/")) {
                throw new IllegalArgumentException("Invalid bucket name");
            }
            if (objectName.contains("../")) {
                throw new IllegalArgumentException("Invalid object name (path traversal detected)");
            }

            LOG.infov("[Storage] Delete initiated - Bucket: {0}, Object: {1}", bucket, objectName);

            // In production:
            // minioClient.removeObject(
            //     RemoveObjectArgs.builder()
            //         .bucket(bucket)
            //         .object(objectName)
            //         .build()
            // );

            LOG.infov("[Storage] Delete successful - Bucket: {0}, Object: {1}", bucket, objectName);

        } catch (IllegalArgumentException e) {
            LOG.warnv("[Storage] Invalid delete request: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.errorv(e, "[Storage] Delete failed for {0}/{1}: {2}", 
                      bucket, objectName, e.getMessage());
            throw new RuntimeException("File deletion failed", e);
        }
    }
}
