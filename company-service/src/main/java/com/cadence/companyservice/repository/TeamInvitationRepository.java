package com.cadence.companyservice.repository;

import com.cadence.companyservice.constant.InvitationStatus;
import com.cadence.companyservice.entity.TeamInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, UUID> {
    Page<TeamInvitation> findAllByCompanyId(UUID companyId, Pageable pageable);
    Optional<TeamInvitation> findByInviteToken(String inviteToken);
    boolean existsByCompanyIdAndEmailIgnoreCaseAndStatus(UUID companyId, String email, InvitationStatus status);
    Optional<TeamInvitation> findFirstByCompanyIdAndEmailIgnoreCaseAndStatus(UUID companyId, String email, InvitationStatus status);
}
