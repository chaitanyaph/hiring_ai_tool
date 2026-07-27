package com.cadence.companyservice.mapper;

import com.cadence.companyservice.dto.response.CompanyResponse;
import com.cadence.companyservice.entity.Company;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    CompanyResponse toResponse(Company company);
}
