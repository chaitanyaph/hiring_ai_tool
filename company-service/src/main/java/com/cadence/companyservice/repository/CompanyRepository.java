package com.cadence.companyservice.repository;

import com.cadence.companyservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByCompanySlug(String companySlug);
    boolean existsByCompanyNameIgnoreCase(String companyName);
}
