package com.cadence.resumeparserservice.util;

import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/** Reads the resume PDF straight out of Resume Service's shared MinIO bucket using coordinates from its /object endpoint. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioObjectReader {

    private final MinioClient minioClient;

    public byte[] readObject(String bucket, String objectName) {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectName).build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to read object [{}/{}] from MinIO: {}", bucket, objectName, e.getMessage(), e);
            throw new ResumeParsingPipelineException("Could not read the resume file from storage: " + e.getMessage(), e);
        }
    }
}
