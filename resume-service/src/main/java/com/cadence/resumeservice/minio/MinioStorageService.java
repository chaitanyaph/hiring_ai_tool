package com.cadence.resumeservice.minio;

import com.cadence.resumeservice.exception.ErrorCode;
import com.cadence.resumeservice.exception.ResumeServiceException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Thin, service-agnostic wrapper around the MinIO SDK -- ResumeService
 * never touches MinioClient directly, so a future swap to S3/GCS only
 * touches this one class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    public void upload(String bucket, String objectName, byte[] content, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload object [{}/{}]: {}", bucket, objectName, e.getMessage(), e);
            throw new ResumeServiceException(ErrorCode.STORAGE_ERROR, "Could not store the resume file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public InputStream download(String bucket, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.error("Failed to download object [{}/{}]: {}", bucket, objectName, e.getMessage(), e);
            throw new ResumeServiceException(ErrorCode.STORAGE_ERROR, "Could not retrieve the resume file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void delete(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.error("Failed to delete object [{}/{}]: {}", bucket, objectName, e.getMessage(), e);
            throw new ResumeServiceException(ErrorCode.STORAGE_ERROR, "Could not delete the resume file from storage", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public StatObjectResponse stat(String bucket, String objectName) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception e) {
            log.error("Failed to stat object [{}/{}]: {}", bucket, objectName, e.getMessage(), e);
            throw new ResumeServiceException(ErrorCode.STORAGE_ERROR, "Could not read the resume file's storage details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** /{candidateId}/{uuid}.pdf -- see README for why this drops the spec's literal companyId segment (a resume isn't company-scoped in this domain). */
    public String buildObjectName(UUID candidateId, String fileExtension) {
        return candidateId + "/" + UUID.randomUUID() + "." + fileExtension;
    }
}
