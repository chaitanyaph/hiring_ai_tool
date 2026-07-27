package com.cadence.authservice.repository;

import com.cadence.authservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsBySlug(String slug);
}
