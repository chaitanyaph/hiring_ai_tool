package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.QuestionStatus;
import com.cadence.codingassessmentservice.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    @Query("""
            SELECT q FROM Question q
            WHERE q.companyId = :companyId
            AND (:difficulty IS NULL OR q.difficulty = :difficulty)
            AND (:status IS NULL OR q.status = :status)
            AND (:keyword IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Question> search(@Param("companyId") UUID companyId, @Param("difficulty") Difficulty difficulty,
                           @Param("status") QuestionStatus status, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT q FROM Question q
            WHERE q.companyId = :companyId AND q.status = com.cadence.codingassessmentservice.constants.QuestionStatus.ACTIVE
            """)
    List<Question> findAllActiveByCompanyId(@Param("companyId") UUID companyId);

    List<Question> findAllByIdIn(List<UUID> ids);
}
