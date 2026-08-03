package com.cadence.resumeservice.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ensures candidate-resumes exists on startup rather than failing on the
 * first upload -- MinIO buckets aren't auto-created, unlike a schema
 * Flyway would migrate for us.
 *
 * Deliberately a separate component from MinioConfig, injected with the
 * already-built MinioClient bean rather than calling MinioConfig's
 * minioClient() method directly: doing that from @PostConstruct on
 * MinioConfig itself threw BeanCurrentlyInCreationException, since
 * @Configuration's CGLIB proxy turns that call into "fetch this bean from
 * the container" while the container is still constructing that very bean.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket [{}]", bucket);
            }
        } catch (Exception e) {
            log.error("Could not verify/create MinIO bucket [{}] on startup: {}", bucket, e.getMessage(), e);
        }
    }
}
