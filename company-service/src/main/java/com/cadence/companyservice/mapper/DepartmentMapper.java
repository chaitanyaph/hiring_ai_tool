package com.cadence.companyservice.mapper;

import com.cadence.companyservice.dto.response.DepartmentResponse;
import com.cadence.companyservice.entity.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    DepartmentResponse toResponse(Department department);
}
