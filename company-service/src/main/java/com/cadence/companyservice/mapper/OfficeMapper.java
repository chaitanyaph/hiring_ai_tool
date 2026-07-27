package com.cadence.companyservice.mapper;

import com.cadence.companyservice.dto.response.OfficeResponse;
import com.cadence.companyservice.entity.Office;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OfficeMapper {
    OfficeResponse toResponse(Office office);
}
