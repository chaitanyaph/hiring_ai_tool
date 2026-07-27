package com.cadence.applicationservice.repository;

import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);
    Optional<Application> findByIdAndCompanyId(UUID id, UUID companyId);
    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    long countByCompanyId(UUID companyId);
    long countByJobId(UUID jobId);

    /** Used by Resume Service to block deleting a resume that's attached to a still-in-flight application. */
    boolean existsByResumeIdAndCurrentStatusNotIn(UUID resumeId, Collection<ApplicationStatus> terminalStatuses);

    /** Used by Resume Service to scope a recruiter's resume preview/download to candidates who applied to their own company. */
    boolean existsByCandidateIdAndCompanyId(UUID candidateId, UUID companyId);
}
