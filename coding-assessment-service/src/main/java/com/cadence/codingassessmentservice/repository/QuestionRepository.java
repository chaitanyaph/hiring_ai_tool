package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.Difficulty;
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
            """)
    Page<Question> search(@Param("companyId") UUID companyId, @Param("difficulty") Difficulty difficulty, Pageable pageable);

    List<Question> findAllByIdIn(List<UUID> ids);
}
