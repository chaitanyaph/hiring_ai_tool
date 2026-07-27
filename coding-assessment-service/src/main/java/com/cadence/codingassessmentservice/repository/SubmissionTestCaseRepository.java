package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.SubmissionTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionTestCaseRepository extends JpaRepository<SubmissionTestCase, UUID> {
    List<SubmissionTestCase> findAllBySubmissionIdOrderByDisplayOrderAsc(UUID submissionId);
}
