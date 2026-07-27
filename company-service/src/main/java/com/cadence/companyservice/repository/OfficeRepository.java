package com.cadence.companyservice.repository;

import com.cadence.companyservice.entity.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OfficeRepository extends JpaRepository<Office, UUID> {
    Page<Office> findAllByCompanyId(UUID companyId, Pageable pageable);
    Optional<Office> findByCompanyIdAndPrimaryOfficeTrue(UUID companyId);
}
