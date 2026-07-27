package com.cadence.resumeservice.service.impl;

import com.cadence.resumeservice.config.RedisConfig;
import com.cadence.resumeservice.constants.PlatformRole;
import com.cadence.resumeservice.constants.ResumeStatus;
import com.cadence.resumeservice.dto.request.RenameResumeRequest;
import com.cadence.resumeservice.dto.response.ResumeObjectDetailsResponse;
import com.cadence.resumeservice.dto.response.ResumeResponse;
import com.cadence.resumeservice.entity.Resume;
import com.cadence.resumeservice.event.ResumeDefaultChangedEvent;
import com.cadence.resumeservice.event.ResumeDeletedEvent;
import com.cadence.resumeservice.event.ResumeUploadedEvent;
import com.cadence.resumeservice.exception.*;
import com.cadence.resumeservice.feign.ApplicationServiceClient;
import com.cadence.resumeservice.feign.CandidateServiceClient;
import com.cadence.resumeservice.feign.dto.CandidateDto;
import com.cadence.resumeservice.kafka.producer.ResumeEventProducer;
import com.cadence.resumeservice.mapper.ResumeMapper;
import com.cadence.resumeservice.minio.MinioStorageService;
import com.cadence.resumeservice.repository.ResumeRepository;
import com.cadence.resumeservice.security.CurrentUser;
import com.cadence.resumeservice.service.ResumeContent;
import com.cadence.resumeservice.service.ResumeService;
import com.cadence.resumeservice.validation.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final FileValidator fileValidator;
    private final MinioStorageService minioStorageService;
    private final CandidateServiceClient candidateServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final ResumeMapper resumeMapper;
    private final ResumeEventProducer eventProducer;
    private final CacheManager cacheManager;

    @Value("${app.resume.max-resumes-per-candidate}")
    private int maxResumesPerCandidate;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Override
    @Transactional
    public ResumeResponse upload(CurrentUser candidate, MultipartFile file, String displayName) {
        fileValidator.validate(file);

        CandidateDto candidateProfile = fetchCandidateOrThrow(candidate.getUserId());
        if (!"ACTIVE".equalsIgnoreCase(candidateProfile.getStatus())) {
            throw new ResumeValidationException(ErrorCode.CANDIDATE_NOT_ACTIVE, "Your candidate account is not active");
        }

        long activeCount = resumeRepository.countByCandidateIdAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE);
        if (activeCount >= maxResumesPerCandidate) {
            throw new ResumeConflictException(ErrorCode.RESUME_LIMIT_EXCEEDED,
                    "You can upload a maximum of " + maxResumesPerCandidate + " resumes");
        }

        byte[] content = readBytes(file);
        String checksum = com.cadence.resumeservice.util.ChecksumUtil.sha256(content);
        if (resumeRepository.existsByCandidateIdAndChecksumAndStatus(candidate.getUserId(), checksum, ResumeStatus.ACTIVE)) {
            throw new ResumeConflictException(ErrorCode.DUPLICATE_RESUME, "This exact resume file has already been uploaded");
        }

        String extension = "pdf";
        String objectName = minioStorageService.buildObjectName(candidate.getUserId(), extension);
        minioStorageService.upload(bucket, objectName, content, file.getContentType());

        boolean isFirstResume = activeCount == 0;
        Resume resume = Resume.builder()
                .candidateId(candidate.getUserId())
                .displayName((displayName == null || displayName.isBlank()) ? stripExtension(file.getOriginalFilename()) : displayName.trim())
                .originalFileName(file.getOriginalFilename())
                .fileExtension(extension)
                .mimeType(file.getContentType())
                .bucketName(bucket)
                .objectName(objectName)
                .checksum(checksum)
                .fileSize(content.length)
                .defaultResume(isFirstResume)
                .status(ResumeStatus.ACTIVE)
                .createdBy(candidate.getUserId())
                .updatedBy(candidate.getUserId())
                .build();
        resume = resumeRepository.save(resume);

        eventProducer.publishResumeUploaded(ResumeUploadedEvent.builder()
                .resumeId(resume.getId()).candidateId(candidate.getUserId())
                .checksum(checksum).occurredAt(LocalDateTime.now()).build());
        if (isFirstResume) {
            eventProducer.publishResumeDefaultChanged(ResumeDefaultChangedEvent.builder()
                    .candidateId(candidate.getUserId()).newDefaultResumeId(resume.getId())
                    .previousDefaultResumeId(null).occurredAt(LocalDateTime.now()).build());
        }
        evictMyResumesCache(candidate.getUserId());

        return resumeMapper.toResponse(resume);
    }

    @Override
    @Cacheable(value = RedisConfig.MY_RESUMES_CACHE, key = "#candidate.userId")
    @Transactional(readOnly = true)
    public List<ResumeResponse> listMyResumes(CurrentUser candidate) {
        return resumeMapper.toResponseList(
                resumeRepository.findAllByCandidateIdAndStatusOrderByUploadedAtDesc(candidate.getUserId(), ResumeStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getDetail(CurrentUser candidate, UUID resumeId) {
        return resumeMapper.toResponse(findOwnedOrThrow(resumeId, candidate.getUserId()));
    }

    @Override
    @Transactional
    public ResumeResponse setDefault(CurrentUser candidate, UUID resumeId) {
        Resume resume = findOwnedActiveOrThrow(resumeId, candidate.getUserId());
        if (resume.isDefaultResume()) {
            return resumeMapper.toResponse(resume);
        }

        UUID previousDefaultId = resumeRepository.findByCandidateIdAndDefaultResumeTrueAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE)
                .map(previous -> {
                    previous.setDefaultResume(false);
                    resumeRepository.save(previous);
                    return previous.getId();
                }).orElse(null);

        resume.setDefaultResume(true);
        resume.setUpdatedBy(candidate.getUserId());
        resume = resumeRepository.save(resume);

        eventProducer.publishResumeDefaultChanged(ResumeDefaultChangedEvent.builder()
                .candidateId(candidate.getUserId()).newDefaultResumeId(resume.getId())
                .previousDefaultResumeId(previousDefaultId).occurredAt(LocalDateTime.now()).build());
        evictMyResumesCache(candidate.getUserId());

        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional
    public ResumeResponse rename(CurrentUser candidate, UUID resumeId, RenameResumeRequest request) {
        Resume resume = findOwnedActiveOrThrow(resumeId, candidate.getUserId());
        resume.setDisplayName(request.getDisplayName().trim());
        resume.setUpdatedBy(candidate.getUserId());
        resume = resumeRepository.save(resume);
        evictMyResumesCache(candidate.getUserId());
        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional
    public void delete(CurrentUser candidate, UUID resumeId) {
        Resume resume = findOwnedActiveOrThrow(resumeId, candidate.getUserId());

        var inUseResponse = applicationServiceClient.isResumeInUse(resumeId);
        if (inUseResponse != null && Boolean.TRUE.equals(inUseResponse.getData())) {
            throw new ResumeConflictException(ErrorCode.RESUME_IN_USE,
                    "This resume is attached to an active job application and cannot be deleted");
        }

        boolean wasDefault = resume.isDefaultResume();
        resume.setStatus(ResumeStatus.DELETED);
        resume.setDefaultResume(false);
        resume.setUpdatedBy(candidate.getUserId());
        resumeRepository.save(resume);

        if (wasDefault) {
            promoteNextDefault(candidate.getUserId());
        }

        eventProducer.publishResumeDeleted(ResumeDeletedEvent.builder()
                .resumeId(resume.getId()).candidateId(candidate.getUserId()).occurredAt(LocalDateTime.now()).build());
        evictMyResumesCache(candidate.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeContent downloadForCandidate(CurrentUser candidate, UUID resumeId) {
        Resume resume = findOwnedActiveOrThrow(resumeId, candidate.getUserId());
        return streamContent(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeContent previewForCandidate(CurrentUser candidate, UUID resumeId) {
        return downloadForCandidate(candidate, resumeId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeContent downloadForRecruiter(CurrentUser recruiter, UUID resumeId) {
        Resume resume = findActiveOrThrow(resumeId);
        requireRecruiterAccess(recruiter, resume.getCandidateId());
        return streamContent(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeContent previewForRecruiter(CurrentUser recruiter, UUID resumeId) {
        return downloadForRecruiter(recruiter, resumeId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getMetadataInternal(UUID resumeId) {
        return resumeMapper.toResponse(findByIdOrThrow(resumeId));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeObjectDetailsResponse getObjectDetailsInternal(UUID resumeId) {
        return resumeMapper.toObjectDetailsResponse(findByIdOrThrow(resumeId));
    }

    @Override
    @Transactional
    public void handleCandidateDeleted(UUID candidateId) {
        // Archived, not purged from MinIO -- this service doesn't own a data-retention/GDPR-erasure
        // policy; physically deleting the object is a deliberate follow-up once that policy exists.
        List<Resume> resumes = resumeRepository.findAllByCandidateId(candidateId);
        for (Resume resume : resumes) {
            if (resume.getStatus() == ResumeStatus.ACTIVE) {
                resume.setStatus(ResumeStatus.ARCHIVED);
                resume.setDefaultResume(false);
                resumeRepository.save(resume);
            }
        }
        evictMyResumesCache(candidateId);
    }

    // ------------------------------------------------------------------

    private void promoteNextDefault(UUID candidateId) {
        List<Resume> remaining = resumeRepository.findAllByCandidateIdAndStatusOrderByUploadedAtDesc(candidateId, ResumeStatus.ACTIVE);
        if (remaining.isEmpty()) {
            return;
        }
        Resume nextDefault = remaining.get(0);
        nextDefault.setDefaultResume(true);
        resumeRepository.save(nextDefault);
        eventProducer.publishResumeDefaultChanged(ResumeDefaultChangedEvent.builder()
                .candidateId(candidateId).newDefaultResumeId(nextDefault.getId())
                .previousDefaultResumeId(null).occurredAt(LocalDateTime.now()).build());
    }

    private ResumeContent streamContent(Resume resume) {
        return new ResumeContent(
                minioStorageService.download(resume.getBucketName(), resume.getObjectName()),
                resume.getOriginalFileName(),
                resume.getMimeType(),
                resume.getFileSize());
    }

    private void requireRecruiterAccess(CurrentUser recruiter, UUID candidateId) {
        if (PlatformRole.ADMIN.equals(recruiter.getRole())) {
            return;
        }
        if (recruiter.getCompanyId() == null) {
            throw new AccessDeniedException("You do not have permission to access this resume");
        }
        var response = applicationServiceClient.hasApplicationFromCandidateToCompany(candidateId, recruiter.getCompanyId());
        boolean hasApplied = response != null && Boolean.TRUE.equals(response.getData());
        if (!hasApplied) {
            throw new AccessDeniedException("This candidate has not applied to your company");
        }
    }

    private Resume findOwnedOrThrow(UUID id, UUID candidateId) {
        return resumeRepository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESUME_NOT_FOUND, "Resume not found"));
    }

    private Resume findOwnedActiveOrThrow(UUID id, UUID candidateId) {
        Resume resume = findOwnedOrThrow(id, candidateId);
        if (resume.getStatus() != ResumeStatus.ACTIVE) {
            throw new ResourceNotFoundException(ErrorCode.RESUME_NOT_FOUND, "Resume not found");
        }
        return resume;
    }

    private Resume findByIdOrThrow(UUID id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESUME_NOT_FOUND, "Resume not found"));
    }

    private Resume findActiveOrThrow(UUID id) {
        Resume resume = findByIdOrThrow(id);
        if (resume.getStatus() != ResumeStatus.ACTIVE) {
            throw new ResourceNotFoundException(ErrorCode.RESUME_NOT_FOUND, "Resume not found");
        }
        return resume;
    }

    private CandidateDto fetchCandidateOrThrow(UUID candidateId) {
        try {
            var response = candidateServiceClient.getCandidateSummary(candidateId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException(ErrorCode.CANDIDATE_NOT_FOUND, "Candidate profile not found");
            }
            return response.getData();
        } catch (feign.FeignException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.CANDIDATE_NOT_FOUND, "Candidate profile not found");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResumeValidationException(ErrorCode.EMPTY_FILE, "Could not read the uploaded file");
        }
    }

    private String stripExtension(String filename) {
        if (filename == null) return "Resume";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private void evictMyResumesCache(UUID candidateId) {
        var cache = cacheManager.getCache(RedisConfig.MY_RESUMES_CACHE);
        if (cache != null) cache.evict(candidateId);
    }
}
