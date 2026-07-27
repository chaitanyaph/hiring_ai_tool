package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.DepartmentRequest;
import com.cadence.companyservice.dto.response.DepartmentResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DepartmentService {
    DepartmentResponse createDepartment(UUID companyId, DepartmentRequest request, String actor);
    PagedResponse<DepartmentResponse> listDepartments(UUID companyId, Pageable pageable);
    DepartmentResponse getDepartment(UUID departmentId);
    DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest request, String actor);
    void deleteDepartment(UUID departmentId, String actor);
}
