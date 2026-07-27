package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.ParserLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParserLogRepository extends JpaRepository<ParserLog, UUID> {
    List<ParserLog> findAllByParsedResumeIdOrderByCreatedAtAsc(UUID parsedResumeId);
}
