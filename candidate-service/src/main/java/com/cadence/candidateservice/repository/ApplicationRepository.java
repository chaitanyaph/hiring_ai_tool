package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.constant.ApplicationStatus;
import com.cadence.candidateservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);
    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    long countByCandidateId(UUID candidateId);
    long countByCandidateIdAndStatusIn(UUID candidateId, Collection<ApplicationStatus> statuses);
    List<Application> findAllByCandidateIdOrderByAppliedAtDesc(UUID candidateId);
    List<Application> findAllByCandidateIdAndStatusInOrderByAppliedAtDesc(UUID candidateId, Collection<ApplicationStatus> statuses);
    List<Application> findTop5ByCandidateIdOrderByAppliedAtDesc(UUID candidateId);

    /** Used by recruiter/hiring-side tooling (e.g. Job Service dashboards) to see applicant volume per job. */
    long countByJobId(UUID jobId);
}
