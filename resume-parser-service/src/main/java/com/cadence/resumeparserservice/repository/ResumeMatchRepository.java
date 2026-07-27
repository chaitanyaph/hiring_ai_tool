package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import com.cadence.resumeparserservice.entity.ResumeMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeMatchRepository extends JpaRepository<ResumeMatch, UUID> {

    Optional<ResumeMatch> findByApplicationId(UUID applicationId);

    /** Rows parked in AWAITING_PARSE for a resume that has just finished parsing. */
    List<ResumeMatch> findAllByResumeIdAndStatus(UUID resumeId, ResumeMatchStatus status);

    List<ResumeMatch> findAllByCandidateIdAndStatusOrderByOverallMatchScoreDesc(UUID candidateId, ResumeMatchStatus status);

    /**
     * Candidate ranking table -- search matches the candidate's parsed
     * name/email (this service's own parsed_resume data), since the
     * Figma search box searches by candidate, and this is the only
     * place that data actually lives without a cross-service join.
     */
    @Query("""
            SELECT rm FROM ResumeMatch rm
            WHERE rm.jobId = :jobId AND rm.status = 'ANALYZED'
            AND (:search IS NULL OR :search = '' OR EXISTS (
                SELECT 1 FROM ParsedResume pr WHERE pr.id = rm.parsedResumeId
                AND (LOWER(pr.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(pr.email) LIKE LOWER(CONCAT('%', :search, '%')))
            ))
            """)
    Page<ResumeMatch> searchByJob(@Param("jobId") UUID jobId, @Param("search") String search, Pageable pageable);

    List<ResumeMatch> findTop10ByJobIdAndStatusOrderByOverallMatchScoreDesc(UUID jobId, ResumeMatchStatus status);

    @Query("""
            SELECT rm FROM ResumeMatch rm
            WHERE rm.status = 'ANALYZED'
            AND (:jobId IS NULL OR rm.jobId = :jobId)
            AND (:departmentId IS NULL OR rm.departmentId = :departmentId)
            AND (:minScore IS NULL OR rm.overallMatchScore >= :minScore)
            ORDER BY rm.overallMatchScore DESC
            """)
    List<ResumeMatch> findRecommendations(@Param("jobId") UUID jobId, @Param("departmentId") UUID departmentId,
                                           @Param("minScore") Integer minScore, Pageable pageable);

    long countByJobIdAndStatus(UUID jobId, ResumeMatchStatus status);

    @Query("SELECT AVG(rm.overallMatchScore) FROM ResumeMatch rm WHERE rm.jobId = :jobId AND rm.status = 'ANALYZED'")
    Double findAvgMatchScoreByJob(@Param("jobId") UUID jobId);

    @Query("SELECT MAX(rm.overallMatchScore) FROM ResumeMatch rm WHERE rm.jobId = :jobId AND rm.status = 'ANALYZED'")
    Integer findTopScoreByJob(@Param("jobId") UUID jobId);

    long countByJobIdAndStatusAndOverallMatchScoreLessThan(UUID jobId, ResumeMatchStatus status, int threshold);
}
