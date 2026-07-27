package com.cadence.resumeparserservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Read-only client against Resume Service's shared MinIO instance --
 * this service never owns a bucket (no ensureBucketExists needed) and
 * never writes an object; bucket/object names are supplied dynamically
 * per call from Resume Service's own /object Feign response.
 */
@Configuration
public class MinioConfig {

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
