package com.cadence.companyservice.repository;

import com.cadence.companyservice.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Page<Department> findAllByCompanyId(UUID companyId, Pageable pageable);
    boolean existsByCompanyIdAndDepartmentNameIgnoreCase(UUID companyId, String departmentName);
}
