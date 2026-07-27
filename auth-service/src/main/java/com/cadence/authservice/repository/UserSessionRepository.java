package com.cadence.authservice.repository;

import com.cadence.authservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findAllByUserIdAndActiveTrue(UUID userId);
}
