package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.entity.ParsedResume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParsedResumeRepository extends JpaRepository<ParsedResume, UUID> {
    Optional<ParsedResume> findByResumeId(UUID resumeId);
    List<ParsedResume> findAllByCandidateId(UUID candidateId);
    boolean existsByResumeIdAndChecksumAndStatus(UUID resumeId, String checksum, ParsingStatus status);

    /**
     * The Figma's "Search candidate…" box matches by name/email once a
     * resume has actually been parsed (that's the only place this
     * service has that data); for QUEUED/PROCESSING rows candidate
     * name display is a frontend/gateway composition against
     * Candidate Service, same as the queue table's "Job" column.
     */
    @Query("""
            SELECT p FROM ParsedResume p
            WHERE (:status IS NULL OR p.status = :status)
            AND (:search IS NULL OR :search = ''
                 OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ParsedResume> search(@Param("status") ParsingStatus status, @Param("search") String search, Pageable pageable);

    long countByStatus(ParsingStatus status);
    long countByStatusAndParsedAtAfter(ParsingStatus status, LocalDateTime after);
}
